package org.vstu.compprehension.adapters;

import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.domains.*;
import org.vstu.compprehension.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.services.LocalizationService;
import org.vstu.compprehension.services.RandomProvider;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Singleton
public class DomainFactoryImpl implements DomainFactory {
    private final @NotNull Map<String, Domain> domains;

    @Autowired
    public DomainFactoryImpl(LocalizationService localizationService,
                             RandomProvider randomProvider,
                             QuestionBank questionStorage) {
        var progExprDomain = new ProgrammingLanguageExpressionDomain(localizationService, randomProvider, questionStorage);
        domains = Stream.<Domain>of(
                        progExprDomain,
                        new ProgrammingLanguageExpressionDTDomain(progExprDomain),
                        new ControlFlowStatementsDomain(localizationService, randomProvider, questionStorage),
                        new ControlFlowDTDomain(randomProvider, localizationService, questionStorage),
                        new ObjectsScopeDTDomain(localizationService, randomProvider, questionStorage),
                        new DataFlowDTDomain(localizationService, randomProvider, questionStorage))
                .collect(Collectors.toUnmodifiableMap(Domain::getDomainId, Function.identity()));
    }

    @Override
    public @NotNull Set<String> getDomainIds() {
        return domains.keySet();
    }

    @Override
    public @NotNull Domain getDomain(@NotNull String domainId) {
        var domain = domains.get(domainId);
        if (domain == null) {
            throw new RuntimeException(String.format("Couldn't resolve domain with id %s", domainId));
        }
        return domain;
    }
}
