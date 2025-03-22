package org.vstu.compprehension.adapters;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.strategies.GradeConfidenceBaseStrategy;
import org.vstu.compprehension.strategies.GradeConfidenceBaseStrategy_Manual50Autogen50;
import org.vstu.compprehension.strategies.StaticStrategy;
import org.vstu.compprehension.strategies.Strategy;
import org.vstu.compprehension.utils.ApplicationContextProvider;
import org.vstu.compprehension.utils.RandomProvider;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component @Singleton
public class StrategyFactory implements AbstractStrategyFactory {
    private @NotNull HashMap<String, Class<? extends AbstractStrategy>> strategyToClassMap = new HashMap<>();

    @Autowired
    public StrategyFactory(@NotNull List<AbstractStrategy> strategies) {
        for (var s : strategies) {
            strategyToClassMap.put(s.getStrategyId(), s.getClass());
        }
    }

    public Set<String> getStrategyIds() {
        return strategyToClassMap.keySet();
    }

    public @NotNull AbstractStrategy getStrategy(@NotNull String strategyId) {
        if (!strategyToClassMap.containsKey(strategyId)) {
            throw new NoSuchBeanDefinitionException(String.format("Couldn't resolve strategy with id %s", strategyId));
        }

        try {
            val clazz = strategyToClassMap.get(strategyId);
            return ApplicationContextProvider.getApplicationContext().getBean(clazz);
        } catch (Exception e) {
            throw new NoSuchBeanDefinitionException(String.format("Couldn't resolve strategy with id %s", strategyId));
        }
    }
}
