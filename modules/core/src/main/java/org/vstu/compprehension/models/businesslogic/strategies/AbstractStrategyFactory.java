package org.vstu.compprehension.models.businesslogic.strategies;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.businesslogic.domains.Domain;

import java.util.Set;

public interface AbstractStrategyFactory {
    Set<String> getStrategyIds();
    @NotNull AbstractStrategy getStrategy(@NotNull String strategyId);
}
