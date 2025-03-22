package org.vstu.compprehension.adapter;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategyFactory;

import java.util.Set;

@Component
public class StrategyFactoryImpl implements AbstractStrategyFactory {
    @Override
    public Set<String> getStrategyIds() {
        return Set.of();
    }

    @NotNull
    @Override
    public AbstractStrategy getStrategy(@NotNull String strategyId) {
        throw new RuntimeException("No strategies supported");
    }
}
