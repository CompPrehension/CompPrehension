package org.vstu.compprehension.service.outbox;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.outbox.OutboxEvent;

/**
 * Обработчик конкретного события.
 */
public interface OutboxEventHandler<E extends OutboxEvent> {

    void handle(@NotNull E event);
}
