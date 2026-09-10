package org.vstu.compprehension.adapters;

import org.vstu.compprehension.businesslogic.domains.ControlFlowDTDomain;
import org.vstu.compprehension.businesslogic.domains.DataFlowDTDomain;
import org.vstu.compprehension.businesslogic.domains.ObjectsScopeDTDomain;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.services.*;
import org.vstu.compprehension.businesslogic.domains.*;
import org.vstu.compprehension.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.repositories.data.DomainDataRepository;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Set;

@Component
@Singleton
public class DomainFactoryImpl implements DomainFactory {
    private @NotNull HashMap<String, Domain> domainIdToClassMap = new HashMap<>();
    private @NotNull HashMap<String, Domain> domainShortNameToClassMap = new HashMap<>(); // TODO remove this

    @Autowired
    public DomainFactoryImpl(DomainDataRepository domainDataRepository,
                             LocalizationService localizationService,
                             RandomProvider randomProvider,
                             QuestionBank questionStorage) {

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
                    progExprDomain);
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
                    localizationService,
                    questionStorage);
            domainIdToClassMap.put(ctrlFlowDomain.getDomainId(), ctrlFlowDomain);
            domainShortNameToClassMap.put(domainData.shortName(), ctrlFlowDomain);
        }
        {
            var objectsScopeDomainData = domains
                    .stream().filter(x -> x.shortName().equals("obj_scope"))
                    .findFirst()
                    .orElseThrow();
            var objectsScopeDomain = new ObjectsScopeDTDomain(
                    objectsScopeDomainData,
                    localizationService,
                    randomProvider,
                    questionStorage);
            domainIdToClassMap.put(objectsScopeDomain.getDomainId(), objectsScopeDomain);
            domainShortNameToClassMap.put(objectsScopeDomainData.shortName(), objectsScopeDomain);
        }
        {
            var dataFlowDomainData = domains
                    .stream().filter(x -> x.shortName().equals("data_flow"))
                    .findFirst()
                    .orElseThrow();
            var dataFlowDomain = new DataFlowDTDomain(
                    dataFlowDomainData,
                    localizationService,
                    randomProvider,
                    questionStorage);
            domainIdToClassMap.put(dataFlowDomain.getDomainId(), dataFlowDomain);
            domainShortNameToClassMap.put(dataFlowDomainData.shortName(), dataFlowDomain);
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
