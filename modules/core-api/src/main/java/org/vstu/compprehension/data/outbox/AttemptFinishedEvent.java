package org.vstu.compprehension.data.outbox;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * Событие завершения упраженения.
 */
@EventType(OutboxEventType.ATTEMPT_FINISHED)
public record AttemptFinishedEvent(long attemptId, double grade, @NotNull Instant finishedAt) implements OutboxEvent {
}
