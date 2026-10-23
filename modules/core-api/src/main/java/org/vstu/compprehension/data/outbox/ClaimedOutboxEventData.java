package org.vstu.compprehension.data.outbox;

import org.jetbrains.annotations.NotNull;

/**
 * Событие, захваченное на обработку.
 */
public record ClaimedOutboxEventData(long id, @NotNull OutboxEventType type, int attempts, @NotNull String payload) {
}
