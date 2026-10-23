package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.outbox.ClaimedOutboxEventData;
import org.vstu.compprehension.data.outbox.OutboxEvent;
import org.vstu.compprehension.data.outbox.OutboxEventType;
import org.vstu.compprehension.repositories.data.OutboxDataRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
class OutboxDataServiceImpl implements OutboxDataService {

    private final OutboxDataRepository outboxDataRepository;

    /** Событие без бизнес-изменения рядом — ошибка: без внешней транзакции публикация падает. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(@NotNull OutboxEvent event) {
        outboxDataRepository.create(event, Instant.now());
    }

    @Transactional
    public @NotNull List<ClaimedOutboxEventData> claimDueEvents(@NotNull Collection<OutboxEventType> types, int limit,
                                                                @NotNull Instant now, @NotNull Instant leaseUntil) {
        return outboxDataRepository.claimDue(types, limit, now, leaseUntil);
    }

    public <E extends OutboxEvent> @NotNull E readEvent(@NotNull ClaimedOutboxEventData claimed,
                                                        @NotNull Class<E> eventClass) {
        return outboxDataRepository.readEvent(claimed, eventClass);
    }

    @Transactional
    public void markProcessed(long eventId, @NotNull Instant processedAt) {
        outboxDataRepository.markProcessed(eventId, processedAt);
    }

    @Transactional
    public void deleteEvent(long eventId) {
        outboxDataRepository.delete(eventId);
    }

    @Transactional
    public void scheduleRetry(long eventId, @NotNull Instant retryAt, @NotNull String error) {
        outboxDataRepository.scheduleRetry(eventId, retryAt, error);
    }

    @Transactional
    public void markFailed(long eventId, @NotNull String error) {
        outboxDataRepository.markFailed(eventId, error);
    }

    @Transactional(readOnly = true)
    public @NotNull List<String> findUnhandledPendingEventTypes(@NotNull Collection<OutboxEventType> handledTypes,
                                                                @NotNull Instant createdBefore) {
        return outboxDataRepository.findUnhandledPendingEventTypes(handledTypes, createdBefore);
    }

    @Transactional(readOnly = true)
    public @NotNull List<String> findBlockedOrderingKeys() {
        return outboxDataRepository.findBlockedOrderingKeys();
    }
}
