package org.vstu.compprehension.mappers;

import org.jetbrains.annotations.NotNull;

/**
 * Интерфейс маппера, заполняющего одну сущность данными другой.
 */
public interface UpdateMapper<S, D> extends Mapping {
    void apply(@NotNull S source, @NotNull D destination);
}
