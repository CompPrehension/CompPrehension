package org.vstu.compprehension.models.businesslogic.strategies;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.utils.ApplicationContextProvider;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;

@Component @Singleton
public class StrategyFactory {
    private @NotNull HashMap<String, String> strategyToClassMap = new HashMap<>();

    @Autowired
    public StrategyFactory(@NotNull List<AbstractStrategy> strategies) {
        for (var s : strategies) {
            strategyToClassMap.put(s.getStrategyId(), s.getClass().getName());
        }
    }

    public @NotNull AbstractStrategy getStrategy(@NotNull String strategyId) {
        if (!strategyToClassMap.containsKey(strategyId)) {
            throw new NoSuchBeanDefinitionException(String.format("Couldn't resolve strategy with id %s", strategyId));
        }

        try {
            val clazz = (Class<AbstractStrategy>)Class.forName(strategyToClassMap.get(strategyId));
            val object = ApplicationContextProvider.getApplicationContext().getBean(clazz);
            return object;
        } catch (Exception e) {
            throw new NoSuchBeanDefinitionException(String.format("Couldn't resolve strategy with id %s", strategyId));
        }
    }
}
