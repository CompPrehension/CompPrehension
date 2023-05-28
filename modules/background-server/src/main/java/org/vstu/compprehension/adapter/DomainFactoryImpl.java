package org.vstu.compprehension.adapter;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;

import java.util.Set;

@Component
public class DomainFactoryImpl implements DomainFactory {
    @NotNull
    @Override
    public Set<String> getDomainIds() {
        throw new RuntimeException("No supported domains");
    }

    @NotNull
    @Override
    public Domain getDomain(@NotNull String domainId) {
        throw new RuntimeException("No supported domains");
    }
}
