package org.vstu.compprehension.models.businesslogic.domains;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.utils.ApplicationContextProvider;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Singleton
public class DomainFactory {
    private @NotNull HashMap<String, Class<?>> domainToClassMap = new HashMap<>();

    @Autowired
    public DomainFactory(@NotNull List<Domain> domains) {
        for (var d : domains) {
            domainToClassMap.put(d.getDomainId(), d.getClass());
        }
    }

    public @NotNull Domain getDomain(@NotNull String domainId) {
        if (!domainToClassMap.containsKey(domainId)) {
            throw new NoSuchBeanDefinitionException(String.format("Couldn't resolve domain with id %s", domainId));
        }

        try {
            val clazz = (Class<Domain>)domainToClassMap.get(domainId);
            val object = ApplicationContextProvider.getApplicationContext().getBean(clazz);
            return object;
        } catch (Exception e) {
            throw new NoSuchBeanDefinitionException(String.format("Couldn't resolve domain with id %s", domainId));
        }
    }
}
