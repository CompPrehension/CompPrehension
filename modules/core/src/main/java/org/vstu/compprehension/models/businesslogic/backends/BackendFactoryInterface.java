package org.vstu.compprehension.models.businesslogic.backends;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import java.util.Set;

public interface BackendFactoryInterface {
    @NotNull Set<String> getBackendIds();
    @NotNull Backend getBackend(@NotNull String backendId) throws NoSuchBeanDefinitionException;

}
