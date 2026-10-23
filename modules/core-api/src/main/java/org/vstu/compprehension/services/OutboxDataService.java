package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.outbox.ClaimedOutboxEventData;
import org.vstu.compprehension.data.outbox.OutboxEvent;
import org.vstu.compprehension.data.outbox.OutboxEventType;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Очередь событий в БД (transactional outbox).
 */
public interface OutboxDataService {

    /** Публикует событие в текущей транзакции. */
    void publish(@NotNull OutboxEvent event);

    /**
     * Захватывает готовые к обработке события указанных типов.
     */
    @NotNull List<ClaimedOutboxEventData> claimDueEvents(@NotNull Collection<OutboxEventType> types, int limit,
                                                         @NotNull Instant now, @NotNull Instant leaseUntil);

    /** Десериализует событие. */
    <E extends OutboxEvent> @NotNull E readEvent(@NotNull ClaimedOutboxEventData claimed, @NotNull Class<E> eventClass);

    void markProcessed(long eventId, @NotNull Instant processedAt);

    void deleteEvent(long eventId);

    void scheduleRetry(long eventId, @NotNull Instant retryAt, @NotNull String error);

    void markFailed(long eventId, @NotNull String error);

    /** Типы событий, которые ждут обработки дольше заданного, но ни один обработчик этого процесса их не берёт. */
    @NotNull List<String> findUnhandledPendingEventTypes(@NotNull Collection<OutboxEventType> handledTypes,
                                                         @NotNull Instant createdBefore);

    /** Ключи порядка, цепочки которых стоят за событием в статусе FAILED. */
    @NotNull List<String> findBlockedOrderingKeys();
}
