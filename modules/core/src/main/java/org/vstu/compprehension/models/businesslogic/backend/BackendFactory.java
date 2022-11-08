package org.vstu.compprehension.models.businesslogic.backend;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Component @RequestScope
public class BackendFactory {
    private HashMap<String, Backend> backends = new HashMap<>();

    @Autowired
    public BackendFactory(@Qualifier("allBackends") List<Backend> backends) {
        for (val b : backends) {
            this.backends.put(b.getBackendId(), b);
        }
    }

    public Set<String> getBackendIds() {
        return backends.keySet();
    }

    public @NotNull Backend getBackend(@NotNull String backendId) {
        var result = backends.get(backendId);
        if (result == null)
            throw new NoSuchBeanDefinitionException(String.format("Can't find backend with id %s", backendId));
        return result;
    }

}
