package org.vstu.compprehension.service.outbox;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.vstu.compprehension.config.OutboxProperties;
import org.vstu.compprehension.data.outbox.ClaimedOutboxEventData;
import org.vstu.compprehension.data.outbox.EventType;
import org.vstu.compprehension.data.outbox.OutboxEvent;
import org.vstu.compprehension.data.outbox.OutboxEventType;
import org.vstu.compprehension.services.OutboxDataService;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Обработчик outbox'a.
 */
@Service
@Log4j2
public class OutboxProcessor {

    /** На это время захваченное событие недоступно другим: если процесс упадёт, событие вернётся в очередь. */
    static final Duration LEASE = Duration.ofMinutes(5);
    static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(30);
    static final Duration MAX_RETRY_DELAY = Duration.ofHours(1);
    /** Событие без обработчика дольше этого времени — повод для предупреждения. */
    static final Duration UNHANDLED_WARNING_AGE = Duration.ofHours(1);

    private final OutboxDataService outbox;
    private final OutboxProperties properties;
    private final Map<OutboxEventType, HandlerBinding<?>> handlers;
    private final Set<OutboxEventType> handledTypes;

    private record HandlerBinding<E extends OutboxEvent>(@NotNull Class<E> eventClass,
                                                         @NotNull OutboxEventHandler<E> handler) {
        void handle(@NotNull OutboxDataService outbox, @NotNull ClaimedOutboxEventData claimed) {
            handler.handle(outbox.readEvent(claimed, eventClass));
        }
    }

    public OutboxProcessor(OutboxDataService outbox, OutboxProperties properties, List<OutboxEventHandler<?>> handlers) {
        this.outbox = outbox;
        this.properties = properties;
        var byType = new EnumMap<OutboxEventType, HandlerBinding<?>>(OutboxEventType.class);
        for (var handler : handlers) {
            var binding = bindHandler(handler);
            var eventType = binding.eventClass().getAnnotation(EventType.class);
            if (eventType == null) {
                throw new IllegalStateException("Event " + binding.eventClass().getName() + " of outbox handler "
                        + ClassUtils.getUserClass(handler).getName() + " has no @" + EventType.class.getSimpleName());
            }
            var type = eventType.value();
            var previous = byType.put(type, binding);
            if (previous != null) {
                throw new IllegalStateException("Two outbox handlers for " + type + ": "
                        + ClassUtils.getUserClass(previous.handler()).getName() + " and "
                        + ClassUtils.getUserClass(handler).getName());
            }
        }
        this.handlers = Collections.unmodifiableMap(byType);
        this.handledTypes = Collections.unmodifiableSet(byType.keySet());
    }

    public void processDueEvents(@NotNull Instant now) {
        if (handledTypes.isEmpty()) {
            return;
        }
        List<ClaimedOutboxEventData> claimed;
        do {
            claimed = outbox.claimDueEvents(handledTypes, properties.getBatchSize(), now, now.plus(LEASE));
            for (var event : claimed) {
                processEvent(event, now);
            }
        } while (!claimed.isEmpty());
    }

    public void reportStuckEvents(@NotNull Instant now) {
        if (!handledTypes.isEmpty()) {
            var unhandledTypes = outbox.findUnhandledPendingEventTypes(handledTypes, now.minus(UNHANDLED_WARNING_AGE));
            if (!unhandledTypes.isEmpty()) {
                log.warn("Outbox events of types {} are pending for over {} and no handler here takes them",
                        unhandledTypes, UNHANDLED_WARNING_AGE);
            }
        }
        var blockedKeys = outbox.findBlockedOrderingKeys();
        if (!blockedKeys.isEmpty()) {
            log.error("Outbox ordering keys {} are blocked by FAILED events: requeue or resolve them manually",
                    blockedKeys);
        }
    }

    private void processEvent(@NotNull ClaimedOutboxEventData claimed, @NotNull Instant now) {
        try {
            handlers.get(claimed.type()).handle(outbox, claimed);
        } catch (RuntimeException ex) {
            recordFailure(claimed, ex, now);
            return;
        }
        if (properties.isKeepProcessedEvents()) {
            outbox.markProcessed(claimed.id(), now);
        } else {
            outbox.deleteEvent(claimed.id());
        }
    }

    private void recordFailure(@NotNull ClaimedOutboxEventData claimed, @NotNull RuntimeException ex,
                               @NotNull Instant now) {
        String error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        if (claimed.attempts() >= properties.getMaxAttempts()) {
            outbox.markFailed(claimed.id(), error);
            log.error("Outbox event {} ({}) failed after {} attempts", claimed.id(), claimed.type(), claimed.attempts(), ex);
            return;
        }
        var retryAt = now.plus(calculateRetryDelay(claimed.attempts()));
        outbox.scheduleRetry(claimed.id(), retryAt, error);
        log.warn("Outbox event {} ({}) attempt {} failed, retry at {}: {}",
                claimed.id(), claimed.type(), claimed.attempts(), retryAt, error);
    }

    /** 30 с, 1 мин, 2 мин, … но не больше часа. */
    private static @NotNull Duration calculateRetryDelay(int attempts) {
        int doublings = Math.min(attempts - 1, 20);
        var delay = INITIAL_RETRY_DELAY.multipliedBy(1L << doublings);
        return delay.compareTo(MAX_RETRY_DELAY) < 0 ? delay : MAX_RETRY_DELAY;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @NotNull HandlerBinding<?> bindHandler(@NotNull OutboxEventHandler<?> handler) {
        // Прокси Spring (например, от @Transactional) — подкласс обработчика, объявление ищется у исходного класса.
        var handlerClass = ClassUtils.getUserClass(handler);
        var eventClass = ResolvableType.forClass(OutboxEventHandler.class, handlerClass).resolveGeneric(0);
        // У лямбды и сырого типа параметр не объявлен: Spring отдаёт null или верхнюю границу — сам OutboxEvent.
        if (eventClass == null || eventClass == OutboxEvent.class) {
            throw new IllegalStateException("Outbox handler " + handlerClass.getName()
                    + " must declare a concrete event class: implements OutboxEventHandler<SomeEvent>");
        }
        // Приведение проверено: eventClass — это и есть параметр типа, с которым объявлен обработчик.
        return new HandlerBinding(eventClass, handler);
    }
}
