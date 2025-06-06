package org.vstu.compprehension.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.adapters.StrategyFactory;
import org.vstu.compprehension.dto.*;
import org.vstu.compprehension.models.businesslogic.Concept;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.Skill;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.UserEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping({"api/refTables" })
public class ReferenceTableController {
    private final DomainFactory domainFactory;
    private final AbstractStrategyFactory strategyFactory;
    private final UserService userService;

    @Autowired
    public ReferenceTableController(DomainFactory domainFactory, StrategyFactory strategyFactory, UserService userService) {
        this.domainFactory = domainFactory;
        this.strategyFactory = strategyFactory;
        this.userService = userService;
    }

    @RequestMapping(value = {"/strategies"}, method = { RequestMethod.GET })
    @ResponseBody
    public List<StrategyDto> getStrategies() {
        var strategyIds = strategyFactory.getStrategyIds();
        var currentLanguage = userService.tryGetCurrentUser()
                .map(UserEntity::getPreferred_language)
                .orElse(Language.ENGLISH);
        return strategyIds.stream()
                .map(strategyFactory::getStrategy)
                .filter(s -> s.getOptions().isVisibleToUser())
                .map(s -> StrategyDto.builder()
                        .id(s.getStrategyId())
                        .displayName(s.getDisplayName(currentLanguage))
                        .description(s.getDescription(currentLanguage))
                        .options(s.getOptions())
                        .build())
                .collect(Collectors.toList());
    }

    @RequestMapping(value = {"/backends"}, method = { RequestMethod.GET })
    @ResponseBody
    public Set<String> getBackends() {
        return Set.of(JenaBackend.BackendId, DecisionTreeReasonerBackend.BACKEND_ID);
    }

    @RequestMapping(value = {"/domains"}, method = { RequestMethod.GET })
    @ResponseBody
    public List<DomainDto> getDomains() throws Exception {
        var domainIds= domainFactory.getDomainIds();
        var currentLanguage = userService.tryGetCurrentUser()
                .map(UserEntity::getPreferred_language)
                .orElse(Language.ENGLISH);
        return domainIds.stream()
                .map(domainFactory::getDomain)
                .map(d -> DomainDto.builder()
                        .id(d.getDomainId())
                        .displayName(d.getDisplayName(currentLanguage))
                        .description(d.getDescription(currentLanguage))
                        .tags(d.getTags().keySet().stream().toList())
                        .concepts(d.getConceptsSimplifiedHierarchy(Concept.FLAG_VISIBLE_TO_TEACHER)
                                .entrySet()
                                .stream()
                                .map(kv -> new ConceptTreeItemDto(
                                        kv.getKey().getName(),
                                        d.getConceptDisplayName(kv.getKey().getName(), currentLanguage),
                                        kv.getKey().getBitflags(),
                                        kv.getValue().stream().map(z -> new ConceptTreeItemDto(
                                                z.getName(),
                                                d.getConceptDisplayName(z.getName(), currentLanguage),
                                                z.getBitflags())
                                        ).toArray(ConceptTreeItemDto[]::new)))
                                .collect(Collectors.toList()))
                        .laws(d.getLawsSimplifiedHierarchy(Law.FLAG_VISIBLE_TO_TEACHER)
                                .entrySet()
                                .stream()
                                .map(kv -> new LawTreeItemDto(
                                        kv.getKey().getName(),
                                        d.getLawDisplayName(kv.getKey().getName(), currentLanguage),
                                        kv.getKey().getBitflags(),
                                        kv.getValue().stream().map(z -> new LawTreeItemDto(
                                                z.getName(),
                                                d.getLawDisplayName(z.getName(), currentLanguage),
                                                z.getBitflags())
                                        ).toArray(LawTreeItemDto[]::new)))
                                .collect(Collectors.toList()))
                        .skills(d.getSkillSimplifiedHierarchy(Skill.FLAG_VISIBLE_TO_TEACHER)
                                .entrySet()
                                .stream()
                                .map(kv -> new SkillTreeItemDto(
                                        kv.getKey().getName(),
                                        d.getSkillDisplayName(kv.getKey().getName(), currentLanguage),
                                        kv.getValue().stream().map(z -> new SkillTreeItemDto(
                                                z.getName(),
                                                d.getLawDisplayName(z.getName(), currentLanguage),
                                                z.getBitflags()
                                                )
                                        ).toArray(SkillTreeItemDto[]::new),
                                        kv.getKey().getBitflags()
                                ))
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    /*
    @RequestMapping(value = {"/domainLaws"}, method = { RequestMethod.GET })
    @ResponseBody
    public List<Law> getDomainLaws(String domaindId) {
        var domain = domainFactory.getDomain(domaindId);
        var laws1 = domain.getPositiveLaws();
        var laws2 = domain.getNegativeLaws();

        return Stream.concat(laws1.stream(), laws2.stream())
                .collect(Collectors.toList());
    }

    @RequestMapping(value = {"/domainConcepts"}, method = { RequestMethod.GET })
    @ResponseBody
    public List<Concept> getConcepts(String domaindId) {
        var domain = domainFactory.getDomain(domaindId);
        return domain.getConcepts();
    }
    */
}
