package org.vstu.compprehension.models.businesslogic.domains;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.utils.ApplicationContextProvider;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public interface DomainFactory {
    @NotNull Set<String> getDomainIds();

    @NotNull Domain getDomain(@NotNull String domainId);
}
