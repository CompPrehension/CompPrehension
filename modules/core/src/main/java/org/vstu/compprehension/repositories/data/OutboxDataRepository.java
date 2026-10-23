package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.outbox.ClaimedOutboxEventData;
import org.vstu.compprehension.data.outbox.EventType;
import org.vstu.compprehension.data.outbox.OutboxEvent;
import org.vstu.compprehension.data.outbox.OutboxEventType;
import org.vstu.compprehension.entities.OutboxEventEntity;
import org.vstu.compprehension.enums.OutboxEventStatus;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.OutboxEventRepository;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxDataRepository {

    private static final int MAX_ERROR_LENGTH = 4000;

    /** Неизвестные поля игнорируются: событие, опубликованное более новой версией, читается старой. */
    private final JsonMapper jsonMapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final OutboxEventRepository outboxEventRepository;
    private final Mapper<OutboxEventEntity, ClaimedOutboxEventData> claimedOutboxEventMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void create(@NotNull OutboxEvent event, @NotNull Instant now) {
        var eventType = event.getClass().getAnnotation(EventType.class);
        if (eventType == null) {
            throw new IllegalArgumentException(
                    "Outbox event " + event.getClass().getName() + " has no @" + EventType.class.getSimpleName());
        }
        var entity = new OutboxEventEntity();
        entity.setEventType(eventType.value().name());
        entity.setPayload(jsonMapper.writeValueAsString(event));
        entity.setOrderingKey(event.getOrderingKey());
        entity.setStatus(OutboxEventStatus.PENDING);
        entity.setAttempts(0);
        entity.setNextAttemptAt(now);
        entity.setCreatedAt(now);
        outboxEventRepository.save(entity);
    }

    @Transactional
    public @NotNull List<ClaimedOutboxEventData> claimDue(@NotNull Collection<OutboxEventType> types, int limit,
                                                          @NotNull Instant now, @NotNull Instant leaseUntil) {
        var events = outboxEventRepository.findDueForUpdateSkipLocked(
                types.stream().map(OutboxEventType::name).toList(), now, limit,
                OutboxEventStatus.PENDING.getCode(), OutboxEventStatus.FAILED.getCode());
        for (var event : events) {
            event.setAttempts(event.getAttempts() + 1);
            event.setNextAttemptAt(leaseUntil);
        }
        return claimedOutboxEventMapper.mapAll(outboxEventRepository.saveAllAndFlush(events));
    }

    public <E extends OutboxEvent> @NotNull E readEvent(@NotNull ClaimedOutboxEventData claimed,
                                                        @NotNull Class<E> eventClass) {
        return jsonMapper.readValue(claimed.payload(), eventClass);
    }

    @Transactional
    public void markProcessed(long eventId, @NotNull Instant processedAt) {
        ensureUpdated(eventId, outboxEventRepository.markProcessed(eventId, OutboxEventStatus.PROCESSED, processedAt));
    }

    @Transactional
    public void delete(long eventId) {
        outboxEventRepository.deleteById(eventId);
        outboxEventRepository.flush();
    }

    @Transactional
    public void scheduleRetry(long eventId, @NotNull Instant retryAt, @NotNull String error) {
        ensureUpdated(eventId, outboxEventRepository.scheduleRetry(eventId, retryAt, truncate(error)));
    }

    @Transactional
    public void markFailed(long eventId, @NotNull String error) {
        ensureUpdated(eventId, outboxEventRepository.markFailed(eventId, OutboxEventStatus.FAILED, truncate(error)));
    }

    @Transactional(readOnly = true)
    public @NotNull List<String> findUnhandledPendingEventTypes(@NotNull Collection<OutboxEventType> handledTypes,
                                                                @NotNull Instant createdBefore) {
        return outboxEventRepository.findUnhandledPendingEventTypes(
                handledTypes.stream().map(OutboxEventType::name).toList(), createdBefore,
                OutboxEventStatus.PENDING.getCode());
    }

    @Transactional(readOnly = true)
    public @NotNull List<String> findBlockedOrderingKeys() {
        return outboxEventRepository.findBlockedOrderingKeys(
                OutboxEventStatus.PENDING.getCode(), OutboxEventStatus.FAILED.getCode());
    }

    private static void ensureUpdated(long eventId, int updatedRows) {
        if (updatedRows != 1) {
            throw new IllegalStateException("Outbox event " + eventId + " not found");
        }
    }

    private static @NotNull String truncate(@NotNull String error) {
        return error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }
}
