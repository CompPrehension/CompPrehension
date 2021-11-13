package org.vstu.compprehension.utils;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.businesslogic.domains.Domain;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class DomainAdapter {
    private static ConcurrentHashMap<String, Domain> domainsCache = new ConcurrentHashMap<>();

    public static @Nullable Domain getDomain(@NotNull String className) {
        if (domainsCache.containsKey(className)) {
            return domainsCache.get(className);
        }

        try {
            val clazz = (Class<Domain>)Class.forName(className);
            val object = ApplicationContextProvider.getApplicationContext().getBean(clazz);
            domainsCache.put(className, object);
            return object;
        } catch (Exception e) {
            return null;
        }
    }
}
