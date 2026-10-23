package org.vstu.compprehension.mappers;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Интерфейс маппера, создающего одну сущность на основе другой.
 */
public interface Mapper<S, D> extends Mapping {

    @NotNull D map(@NotNull S source);

    default @NotNull List<D> mapAll(@NotNull Collection<? extends S> sources) {
        return sources.stream().map(this::map).toList();
    }
}
