package org.vstu.compprehension.adapters;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategyFactory;

import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component @Singleton
public class StrategyFactory implements AbstractStrategyFactory {
    private final @NotNull Map<String, AbstractStrategy> strategiesById = new HashMap<>();

    @Autowired
    public StrategyFactory(@NotNull List<AbstractStrategy> strategies) {
        for (var s : strategies) {
            strategiesById.put(s.getStrategyId(), s);
        }
    }

    @Override
    public Set<String> getStrategyIds() {
        return strategiesById.keySet();
    }

    @Override
    public @NotNull AbstractStrategy getStrategy(@NotNull String strategyId) {
        var strategy = strategiesById.get(strategyId);
        if (strategy == null) {
            throw new NoSuchBeanDefinitionException(String.format("Couldn't resolve strategy with id %s", strategyId));
        }
        return strategy;
    }
}
