package org.vstu.compprehension.adapters;

import com.google.common.collect.Lists;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.domains.ControlFlowStatementsDomain;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.repository.DomainRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.utils.RandomProvider;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Set;

@Component
@Singleton
public class DomainFactoryImpl implements DomainFactory {
    private @NotNull HashMap<String, Domain> domainToClassMap = new HashMap<>();

    @Autowired
    public DomainFactoryImpl(DomainRepository domainRepository,
                             LocalizationService localizationService,
                             RandomProvider randomProvider,
                             QuestionMetadataRepository questionMetadataRepository) {

        var domains = Lists.newArrayList(domainRepository.findAll());
        {
            var progExprDomainEntity = domains
                    .stream().filter(x -> x.getShortName().equals("expression"))
                    .findFirst()
                    .orElseThrow();
            var progExprDomain = new ProgrammingLanguageExpressionDomain(
                    progExprDomainEntity,
                    localizationService,
                    randomProvider,
                    questionMetadataRepository);
            domainToClassMap.put(progExprDomain.getDomainId(), progExprDomain);
        }
        {
            var controlFlowDomainEntity = domains
                    .stream().filter(x -> x.getShortName().equals("ctrl_flow"))
                    .findFirst()
                    .orElseThrow();
            var controlFlowDomain = new ControlFlowStatementsDomain(
                    controlFlowDomainEntity,
                    localizationService,
                    randomProvider,
                    questionMetadataRepository);
            domainToClassMap.put(controlFlowDomain.getDomainId(), controlFlowDomain);
        }

    }

    @NotNull
    public Set<String> getDomainIds() {
        return domainToClassMap.keySet();
    }

    public @NotNull Domain getDomain(@NotNull String domainId) {
        if (!domainToClassMap.containsKey(domainId)) {
            throw new NoSuchBeanDefinitionException(String.format("Couldn't resolve domain with id %s", domainId));
        }

        try {
            val domain = domainToClassMap.get(domainId);
            return domain;
        } catch (Exception e) {
            throw new NoSuchBeanDefinitionException(String.format("Couldn't resolve domain with id %s", domainId));
        }
    }
}
