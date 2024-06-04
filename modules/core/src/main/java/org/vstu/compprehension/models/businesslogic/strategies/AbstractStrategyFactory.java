package org.vstu.compprehension.models.businesslogic.strategies;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface AbstractStrategyFactory {
    Set<String> getStrategyIds();
    @NotNull AbstractStrategy getStrategy(@NotNull String strategyId);
}
