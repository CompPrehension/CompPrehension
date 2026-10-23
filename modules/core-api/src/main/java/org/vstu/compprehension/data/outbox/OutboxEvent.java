package org.vstu.compprehension.data.outbox;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.Nullable;

/**
 * Базовый класс события.
 */
public interface OutboxEvent {

    /**
     * Ключ сортировки события (задается только если для обработки выжен порядок).
     */
    @JsonIgnore
    default @Nullable String getOrderingKey() {
        return null;
    }
}
