package org.vstu.compprehension.service.outbox;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.vstu.compprehension.config.OutboxProperties;
import org.vstu.compprehension.data.outbox.AttemptFinishedEvent;
import org.vstu.compprehension.data.outbox.OutboxEventType;
import org.vstu.compprehension.entities.OutboxEventEntity;
import org.vstu.compprehension.enums.OutboxEventStatus;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.repositories.entity.OutboxEventRepository;
import org.vstu.compprehension.services.OutboxDataService;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxProcessorTest extends AbstractIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired private OutboxDataService outboxDataService;
    @Autowired private OutboxProperties outboxProperties;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private final RecordingHandler handler = new RecordingHandler();

    /** Запоминает номера обработанных событий; для номеров из {@code failing} падает. */
    static class RecordingHandler implements OutboxEventHandler<AttemptFinishedEvent> {
        final List<Long> handled = new ArrayList<>();
        final Set<Long> failing = new HashSet<>();

        @Override
        public void handle(@NotNull AttemptFinishedEvent event) {
            if (failing.contains(event.attemptId())) {
                throw new IllegalStateException("handler failed for " + event.attemptId());
            }
            handled.add(event.attemptId());
        }
    }

    @AfterEach
    void cleanUp() {
        outboxEventRepository.deleteAllInBatch();
    }

    /** Опубликованное и зафиксированное событие обрабатывается и остаётся в истории. */
    @Test
    void committedEventIsHandled() {
        // Arrange.
        publishInTransaction(1);

        // Act.
        createProcessor().processDueEvents(Instant.now());

        // Assert.
        assertEquals(List.of(1L), handler.handled);
        var event = requireEvent(1);
        assertEquals(OutboxEventStatus.PROCESSED, event.getStatus());
        assertNotNull(event.getProcessedAt());
    }

    /** Откат транзакции убирает и событие: обрабатывать нечего. */
    @Test
    void rolledBackEventIsNotHandled() {
        // Arrange.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            outboxDataService.publish(new AttemptFinishedEvent(1, 1.0, Instant.now()));
            status.setRollbackOnly();
        });

        // Act.
        createProcessor().processDueEvents(Instant.now());

        // Assert.
        assertTrue(findEvent(1).isEmpty());
        assertTrue(handler.handled.isEmpty());
    }

    /** Публиковать событие без транзакции нельзя: оно всегда идёт вместе с бизнес-изменением. */
    @Test
    void publishOutsideTransactionFails() {
        // Act & Assert.
        assertThrows(IllegalTransactionStateException.class,
                () -> outboxDataService.publish(new AttemptFinishedEvent(1, 1.0, Instant.now())));
    }

    /** Упавшее событие хранит причину, не берётся раньше срока повтора и обрабатывается повтором. */
    @Test
    void failedEventIsRetriedLater() {
        // Arrange.
        publishInTransaction(1);
        handler.failing.add(1L);
        var processor = createProcessor();
        var now = Instant.now();

        // Act.
        processor.processDueEvents(now);
        var afterFailure = requireEvent(1);
        processor.processDueEvents(now);
        handler.failing.clear();
        processor.processDueEvents(now.plus(OutboxProcessor.INITIAL_RETRY_DELAY));

        // Assert.
        assertEquals(OutboxEventStatus.PENDING, afterFailure.getStatus());
        assertEquals(1, afterFailure.getAttempts());
        assertEquals("IllegalStateException: handler failed for 1", afterFailure.getLastError());
        assertEquals(List.of(1L), handler.handled);
        var processed = requireEvent(1);
        assertEquals(OutboxEventStatus.PROCESSED, processed.getStatus());
        assertEquals(2, processed.getAttempts());
    }

    /** После исчерпания попыток событие FAILED и больше не обрабатывается. */
    @Test
    void eventBecomesFailedAfterMaxAttempts() {
        // Arrange.
        publishInTransaction(1);
        handler.failing.add(1L);
        var processor = createProcessor();
        var now = Instant.now();
        int maxAttempts = outboxProperties.getMaxAttempts();

        // Act.
        for (int attempt = 0; attempt <= maxAttempts; attempt++) {
            // Шаг больше самой длинной задержки повтора: каждый раз событие уже готово к обработке.
            processor.processDueEvents(now.plus(OutboxProcessor.MAX_RETRY_DELAY.multipliedBy(2L * attempt)));
        }

        // Assert.
        var event = requireEvent(1);
        assertEquals(OutboxEventStatus.FAILED, event.getStatus());
        assertEquals(maxAttempts, event.getAttempts());
    }

    /** События с одним ключом порядка обрабатываются по очереди: следующее ждёт, пока не обработано предыдущее. */
    @Test
    void eventsWithSameOrderingKeyAreHandledInOrder() {
        // Arrange.
        saveOrderedEvent(1, "chain");
        saveOrderedEvent(2, "chain");
        handler.failing.add(1L);
        var processor = createProcessor();
        var now = Instant.now();

        // Act.
        processor.processDueEvents(now);
        var whileFirstFails = List.copyOf(handler.handled);
        handler.failing.clear();
        processor.processDueEvents(now.plus(OutboxProcessor.INITIAL_RETRY_DELAY));

        // Assert.
        assertTrue(whileFirstFails.isEmpty());
        assertEquals(List.of(1L, 2L), handler.handled);
    }

    /** FAILED-событие блокирует свою цепочку: следующие события с тем же ключом не обрабатываются. */
    @Test
    void failedEventBlocksItsOrderingChain() {
        // Arrange.
        saveOrderedEvent(1, "chain");
        saveOrderedEvent(2, "chain");
        handler.failing.add(1L);
        var processor = createProcessor();
        var now = Instant.now();

        // Act.
        for (int attempt = 0; attempt <= outboxProperties.getMaxAttempts(); attempt++) {
            processor.processDueEvents(now.plus(OutboxProcessor.MAX_RETRY_DELAY.multipliedBy(2L * attempt)));
        }

        // Assert.
        assertEquals(OutboxEventStatus.FAILED, requireEvent(1).getStatus());
        var second = requireEvent(2);
        assertEquals(OutboxEventStatus.PENDING, second.getStatus());
        assertEquals(0, second.getAttempts());
        assertTrue(handler.handled.isEmpty());
        assertEquals(List.of("chain"), outboxDataService.findBlockedOrderingKeys());
    }

    /** Событие типа, для которого у процесса нет обработчика, не захватывается и попыток не тратит. */
    @Test
    void eventWithoutHandlerIsNotClaimed() {
        // Arrange.
        var now = Instant.now();
        var event = new OutboxEventEntity();
        event.setEventType("EVENT_FROM_NEWER_VERSION");
        event.setPayload("{}");
        event.setStatus(OutboxEventStatus.PENDING);
        event.setNextAttemptAt(now);
        event.setCreatedAt(now);
        long eventId = outboxEventRepository.save(event).getId();

        // Act.
        createProcessor().processDueEvents(now);

        // Assert.
        var stored = outboxEventRepository.findById(eventId).orElseThrow();
        assertEquals(OutboxEventStatus.PENDING, stored.getStatus());
        assertEquals(0, stored.getAttempts());
        assertEquals(List.of("EVENT_FROM_NEWER_VERSION"), outboxDataService.findUnhandledPendingEventTypes(
                List.of(OutboxEventType.ATTEMPT_FINISHED), now.plus(Duration.ofSeconds(1))));
    }

    /** Если хранить историю не нужно, обработанное событие удаляется. */
    @Test
    void processedEventIsDeletedWhenHistoryIsNotKept() {
        // Arrange.
        publishInTransaction(1);
        var properties = new OutboxProperties();
        properties.setKeepProcessedEvents(false);
        var processor = new OutboxProcessor(outboxDataService, properties, List.of(handler));

        // Act.
        processor.processDueEvents(Instant.now());

        // Assert.
        assertEquals(List.of(1L), handler.handled);
        assertTrue(findEvent(1).isEmpty());
    }

    private @NotNull OutboxProcessor createProcessor() {
        return new OutboxProcessor(outboxDataService, outboxProperties, List.of(handler));
    }

    private void publishInTransaction(long number) {
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> outboxDataService.publish(new AttemptFinishedEvent(number, 1.0, Instant.now())));
    }

    /** Кладёт событие прямо в таблицу: у события оценки ключа порядка нет, а механизм порядка общий. */
    private void saveOrderedEvent(long number, @NotNull String orderingKey) {
        var now = Instant.now();
        var event = new OutboxEventEntity();
        event.setEventType(OutboxEventType.ATTEMPT_FINISHED.name());
        event.setPayload(JSON.writeValueAsString(new AttemptFinishedEvent(number, 1.0, now)));
        event.setOrderingKey(orderingKey);
        event.setStatus(OutboxEventStatus.PENDING);
        event.setNextAttemptAt(now);
        event.setCreatedAt(now);
        outboxEventRepository.save(event);
    }

    private @NotNull OutboxEventEntity requireEvent(long number) {
        return findEvent(number).orElseThrow(() -> new AssertionError("No outbox event " + number));
    }

    private @NotNull Optional<OutboxEventEntity> findEvent(long number) {
        return outboxEventRepository.findAll().stream()
                .filter(event -> event.getEventType().equals(OutboxEventType.ATTEMPT_FINISHED.name()))
                .filter(event -> JSON.readValue(event.getPayload(), AttemptFinishedEvent.class).attemptId() == number)
                .findFirst();
    }
}
