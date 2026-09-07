package org.vstu.compprehension.frontend;

import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.Law;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.ConceptTreeItemDto;
import org.vstu.compprehension.frontend.dto.DomainDto;
import org.vstu.compprehension.frontend.dto.LawTreeItemDto;
import org.vstu.compprehension.frontend.dto.SkillTreeItemDto;
import org.vstu.compprehension.frontend.dto.StrategyDto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ReferenceDataFacadeImpl implements ReferenceTableFrontendService {
    private final DomainFactory domainFactory;
    private final AbstractStrategyFactory strategyFactory;

    public ReferenceDataFacadeImpl(DomainFactory domainFactory, AbstractStrategyFactory strategyFactory) {
        this.domainFactory = domainFactory;
        this.strategyFactory = strategyFactory;
    }

    @Override
    public List<StrategyDto> getStrategies(Language language) {
        var strategyIds = strategyFactory.getStrategyIds();
        return strategyIds.stream()
                .map(strategyFactory::getStrategy)
                .filter(s -> s.getOptions().isVisibleToUser())
                .map(s -> StrategyDto.builder()
                        .id(s.getStrategyId())
                        .displayName(s.getDisplayName(language))
                        .description(s.getDescription(language))
                        .options(s.getOptions())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getBackendIds() {
        // TODO get backend dor specified domain
        return domainFactory.getDomainIds()
                .stream().map(domainFactory::getDomain)
                .map(Domain::getBackendId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<DomainDto> getDomains(Language language) {
        var domainIds = domainFactory.getDomainIds();
        return domainIds.stream()
                .map(domainFactory::getDomain)
                .map(d -> DomainDto.builder()
                        .id(d.getDomainId())
                        .displayName(d.getDisplayName(language))
                        .description(d.getDescription(language))
                        .tags(d.getTags().keySet().stream().toList())
                        .concepts(d.getConceptsSimplifiedHierarchy(Concept.FLAG_VISIBLE_TO_TEACHER)
                                .entrySet()
                                .stream()
                                .map(kv -> new ConceptTreeItemDto(
                                        kv.getKey().getName(),
                                        d.getConceptDisplayName(kv.getKey().getName(), language),
                                        kv.getKey().getBitflags(),
                                        kv.getValue().stream().map(z -> new ConceptTreeItemDto(
                                                z.getName(),
                                                d.getConceptDisplayName(z.getName(), language),
                                                z.getBitflags())
                                        ).toArray(ConceptTreeItemDto[]::new)))
                                .collect(Collectors.toList()))
                        .laws(d.getLawsSimplifiedHierarchy(Law.FLAG_VISIBLE_TO_TEACHER)
                                .entrySet()
                                .stream()
                                .map(kv -> new LawTreeItemDto(
                                        kv.getKey().getName(),
                                        d.getLawDisplayName(kv.getKey().getName(), language),
                                        kv.getKey().getBitflags(),
                                        kv.getValue().stream().map(z -> new LawTreeItemDto(
                                                z.getName(),
                                                d.getLawDisplayName(z.getName(), language),
                                                z.getBitflags())
                                        ).toArray(LawTreeItemDto[]::new)))
                                .collect(Collectors.toList()))
                        .skills(d.getSkillSimplifiedHierarchy(Skill.FLAG_VISIBLE_TO_TEACHER)
                                .entrySet()
                                .stream()
                                .map(kv -> new SkillTreeItemDto(
                                        kv.getKey().getName(),
                                        d.getSkillDisplayName(kv.getKey().getName(), language),
                                        kv.getValue().stream().map(z -> new SkillTreeItemDto(
                                                z.getName(),
                                                d.getLawDisplayName(z.getName(), language),
                                                z.getBitflags()
                                                )
                                        ).toArray(SkillTreeItemDto[]::new),
                                        kv.getKey().getBitflags()
                                ))
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }
}
