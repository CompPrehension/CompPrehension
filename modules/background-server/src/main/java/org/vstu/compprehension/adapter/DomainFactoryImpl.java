package org.vstu.compprehension.adapter;

import org.vstu.compprehension.models.businesslogic.domains.ControlFlowDTDomain;
import org.vstu.compprehension.models.businesslogic.domains.DataFlowDTDomain;
import org.vstu.compprehension.models.businesslogic.domains.ObjectsScopeDTDomain;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.Service.SupplementaryStepService;
import org.vstu.compprehension.Service.ExerciseAttemptService;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.domains.*;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.repository.data.DomainDataRepository;
import org.vstu.compprehension.utils.RandomProvider;

import java.util.HashMap;
import java.util.Set;

@Component
public class DomainFactoryImpl implements DomainFactory {
    private @NotNull HashMap<String, Domain> domainIdToClassMap = new HashMap<>();
    private @NotNull HashMap<String, Domain> domainShortNameToClassMap = new HashMap<>(); // TODO remove this

    @Autowired
    public DomainFactoryImpl(DomainDataRepository domainDataRepository,
                             LocalizationService localizationService,
                             RandomProvider randomProvider,
                             QuestionBank questionStorage,
                             ExerciseAttemptService exerciseAttemptService,
                             SupplementaryStepService supplementaryStepService) {

        var domains = domainDataRepository.findAll();
        {
            var progExprDomainData = domains
                    .stream().filter(x -> x.shortName().equals("expression"))
                    .findFirst()
                    .orElseThrow();
            var progExprDomain = new ProgrammingLanguageExpressionDomain(
                    progExprDomainData,
                    localizationService,
                    randomProvider,
                    exerciseAttemptService,
                    supplementaryStepService,
                    questionStorage);
            domainIdToClassMap.put(progExprDomain.getDomainId(), progExprDomain);
            domainShortNameToClassMap.put(progExprDomainData.shortName(), progExprDomain);
        }
        {
            var controlFlowDomainData = domains
                    .stream().filter(x -> x.shortName().equals("ctrl_flow"))
                    .findFirst()
                    .orElseThrow();
            var controlFlowDomain = new ControlFlowStatementsDomain(
                    controlFlowDomainData,
                    localizationService,
                    randomProvider,
                    exerciseAttemptService,
                    supplementaryStepService,
                    questionStorage);
            domainIdToClassMap.put(controlFlowDomain.getDomainId(), controlFlowDomain);
            domainShortNameToClassMap.put(controlFlowDomainData.shortName(), controlFlowDomain);
        }
        {
            var progExprDomain = (ProgrammingLanguageExpressionDomain)domainShortNameToClassMap.get("expression");
            var dtDomainData = domains
                    .stream().filter(x -> x.shortName().equals("expression_dt"))
                    .findFirst()
                    .orElseThrow();
            var dtDomain = new ProgrammingLanguageExpressionDTDomain(
                    dtDomainData,
                    progExprDomain,
                    exerciseAttemptService,
                    supplementaryStepService);
            domainIdToClassMap.put(dtDomain.getDomainId(), dtDomain);
            domainShortNameToClassMap.put(dtDomainData.shortName(), dtDomain);
        }
        {
            var domainData = domains
                    .stream().filter(x -> x.shortName().equals("ctrl_flow_dt25"))
                    .findFirst()
                    .orElseThrow();
            var ctrlFlowDomain = new ControlFlowDTDomain(
                    domainData,
                    randomProvider,
                    exerciseAttemptService,
                    supplementaryStepService,
                    localizationService,
                    questionStorage);
            domainIdToClassMap.put(ctrlFlowDomain.getDomainId(), ctrlFlowDomain);
            domainShortNameToClassMap.put(domainData.shortName(), ctrlFlowDomain);
        }
    }

    @Override
    @NotNull
    public Set<String> getDomainIds() {
        return domainIdToClassMap.keySet();
    }

    @Override
    public @NotNull Domain getDomain(@NotNull String domainId) {
        if (!domainIdToClassMap.containsKey(domainId) && !domainShortNameToClassMap.containsKey(domainId)) {
            throw new RuntimeException(String.format("Couldn't resolve domain with id %s", domainId));
        }

        var domain = domainIdToClassMap.get(domainId);
        if (domain == null) {
            domain = domainShortNameToClassMap.get(domainId);
        }
        if (domain == null) {
            throw new RuntimeException(String.format("Couldn't resolve domain with id %s", domainId));
        }
        return domain;
    }
}
