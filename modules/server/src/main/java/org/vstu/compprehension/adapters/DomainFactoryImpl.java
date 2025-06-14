package org.vstu.compprehension.adapters;

import com.google.common.collect.Lists;
import domains.DataFlowDTDomain;
import domains.ObjectsScopeDTDomain;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.domains.*;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.repository.DomainRepository;
import org.vstu.compprehension.utils.RandomProvider;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Set;

@Component
@Singleton
public class DomainFactoryImpl implements DomainFactory {
    private @NotNull HashMap<String, Domain> domainIdToClassMap = new HashMap<>();
    private @NotNull HashMap<String, Domain> domainShortNameToClassMap = new HashMap<>(); // TODO remove this

    @Autowired
    public DomainFactoryImpl(DomainRepository domainRepository,
                             LocalizationService localizationService,
                             RandomProvider randomProvider,
                             QuestionBank questionStorage) {

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
                    questionStorage);
            domainIdToClassMap.put(progExprDomain.getDomainId(), progExprDomain);
            domainShortNameToClassMap.put(progExprDomainEntity.getShortName(), progExprDomain);
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
                    questionStorage);
            domainIdToClassMap.put(controlFlowDomain.getDomainId(), controlFlowDomain);
            domainShortNameToClassMap.put(controlFlowDomainEntity.getShortName(), controlFlowDomain);
        }
        {
            var progExprDomain = (ProgrammingLanguageExpressionDomain)domainShortNameToClassMap.get("expression");
            var dtDomainEntity = domains
                    .stream().filter(x -> x.getShortName().equals("expression_dt"))
                    .findFirst()
                    .orElseThrow();
            var dtDomain = new ProgrammingLanguageExpressionDTDomain(
                    dtDomainEntity,
                    progExprDomain);
            domainIdToClassMap.put(dtDomain.getDomainId(), dtDomain);
            domainShortNameToClassMap.put(dtDomainEntity.getShortName(), dtDomain);
        }
        {
            var controlFlowDomain = (ControlFlowStatementsDomain)domainShortNameToClassMap.get("ctrl_flow");
            var dtDomainEntity = domains
                    .stream().filter(x -> x.getName/*!*/().equals("ControlFlowStatementsDTDomain"))
                    .findFirst()
                    .orElseThrow();
            var dtDomain = new ControlFlowStatementsDTDomain(
                    dtDomainEntity,
                    controlFlowDomain);
            domainIdToClassMap.put(dtDomain.getDomainId(), dtDomain);
            domainShortNameToClassMap.put(dtDomainEntity.getShortName(), dtDomain);
        }
        {
            var objectsScopeDomainEntity = domains
                    .stream().filter(x -> x.getShortName().equals("obj_scope"))
                    .findFirst()
                    .orElseThrow();
            var objectsScopeDomain = new ObjectsScopeDTDomain(
                    objectsScopeDomainEntity,
                    localizationService,
                    randomProvider,
                    questionStorage);
            domainIdToClassMap.put(objectsScopeDomain.getDomainId(), objectsScopeDomain);
            domainShortNameToClassMap.put(objectsScopeDomainEntity.getShortName(), objectsScopeDomain);
        }
        {
            var dataFlowDomainEntity = domains
                    .stream().filter(x -> x.getShortName().equals("data_flow"))
                    .findFirst()
                    .orElseThrow();
            var dataFlowDomain = new DataFlowDTDomain(
                    dataFlowDomainEntity,
                    localizationService,
                    randomProvider,
                    questionStorage);
            domainIdToClassMap.put(dataFlowDomain.getDomainId(), dataFlowDomain);
            domainShortNameToClassMap.put(dataFlowDomainEntity.getShortName(), dataFlowDomain);
        }

    }

    @NotNull
    public Set<String> getDomainIds() {
        return domainIdToClassMap.keySet();
    }

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
