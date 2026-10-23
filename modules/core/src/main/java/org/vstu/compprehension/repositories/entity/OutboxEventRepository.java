package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.OutboxEventEntity;
import org.vstu.compprehension.enums.OutboxEventStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    /**
     * Блокирует строки до конца транзакции; занятые другим обработчиком пропускаются (SKIP LOCKED).
     * Подзапрос читает без блокировок: из цепочки с одним ключом порядка выбирается только её голова,
     * а голова, занятая другим обработчиком, остаётся PENDING и не пускает остальных.
     */
    @Query(value = """
            select e.* from outbox_event e
            where e.status = :pending
              and e.next_attempt_at <= :now
              and e.event_type in (:types)
              and (e.ordering_key is null or not exists (
                    select 1 from outbox_event p
                    where p.ordering_key = e.ordering_key
                      and p.id < e.id
                      and p.status in (:pending, :failed)))
            order by e.id
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEventEntity> findDueForUpdateSkipLocked(@Param("types") Collection<String> types,
                                                       @Param("now") Instant now,
                                                       @Param("limit") int limit,
                                                       @Param("pending") int pendingCode,
                                                       @Param("failed") int failedCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OutboxEventEntity e set e.status = :status, e.processedAt = :processedAt, e.lastError = null where e.id = :id")
    int markProcessed(@Param("id") long id, @Param("status") OutboxEventStatus processed,
                      @Param("processedAt") Instant processedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OutboxEventEntity e set e.nextAttemptAt = :retryAt, e.lastError = :error where e.id = :id")
    int scheduleRetry(@Param("id") long id, @Param("retryAt") Instant retryAt, @Param("error") String error);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OutboxEventEntity e set e.status = :status, e.lastError = :error where e.id = :id")
    int markFailed(@Param("id") long id, @Param("status") OutboxEventStatus failed, @Param("error") String error);

    @Query(value = """
            select distinct e.event_type from outbox_event e
            where e.status = :pending
              and e.created_at < :createdBefore
              and e.event_type not in (:handledTypes)
            """, nativeQuery = true)
    List<String> findUnhandledPendingEventTypes(@Param("handledTypes") Collection<String> handledTypes,
                                                @Param("createdBefore") Instant createdBefore,
                                                @Param("pending") int pendingCode);

    @Query(value = """
            select distinct f.ordering_key from outbox_event f
            where f.status = :failed
              and f.ordering_key is not null
              and exists (
                    select 1 from outbox_event p
                    where p.ordering_key = f.ordering_key
                      and p.id > f.id
                      and p.status = :pending)
            """, nativeQuery = true)
    List<String> findBlockedOrderingKeys(@Param("pending") int pendingCode, @Param("failed") int failedCode);
}
