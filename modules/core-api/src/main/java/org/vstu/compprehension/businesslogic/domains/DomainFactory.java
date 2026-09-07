package org.vstu.compprehension.businesslogic.domains;

import org.jetbrains.annotations.NotNull;
import java.util.Set;

public interface DomainFactory {
    @NotNull Set<String> getDomainIds();

    @NotNull Domain getDomain(@NotNull String domainId);
}
