package org.vstu.compprehension.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.Service.AuthorizationService;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.adapters.StrategyFactory;
import org.vstu.compprehension.dto.ConceptTreeItemDto;
import org.vstu.compprehension.dto.DomainDto;
import org.vstu.compprehension.dto.LawTreeItemDto;
import org.vstu.compprehension.dto.StrategyDto;
import org.vstu.compprehension.models.businesslogic.Concept;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects;
import org.vstu.compprehension.models.businesslogic.backend.BackendFactory;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.UserEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping({"api/refTables" })
public class ReferenceTableController {
    private final DomainFactory domainFactory;
    private final AbstractStrategyFactory strategyFactory;
    private final BackendFactory backendFactory;
    private final UserService userService;
    private final CourseService courseService;
    private final AuthorizationService authorizationService;

    @Autowired
    public ReferenceTableController(DomainFactory domainFactory, StrategyFactory strategyFactory, BackendFactory backendFactory, UserService userService, CourseService courseService, AuthorizationService authorizationService) {
        this.domainFactory = domainFactory;
        this.strategyFactory = strategyFactory;
        this.backendFactory = backendFactory;
        this.userService = userService;
        this.courseService = courseService;
        this.authorizationService = authorizationService;
    }

    @RequestMapping(value = {"/strategies"}, method = { RequestMethod.GET })
    @ResponseBody
    public List<StrategyDto> getStrategies() throws Exception {
        if (!isAuthorized(AuthObjects.Permissions.ViewExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

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

    private boolean isAuthorized(String permissionName) throws Exception {
        var course = courseService.getCurrentCourse();
        var userId = userService.getCurrentUser().getId();

        return authorizationService.isAuthorizedCourse(
                userId,
                permissionName,
                course.getId()
        );
    }

    @RequestMapping(value = {"/backends"}, method = { RequestMethod.GET })
    @ResponseBody
    public Set<String> getBackends() throws Exception {
        if (!isAuthorized(AuthObjects.Permissions.ViewExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        return backendFactory.getBackendIds();
    }

    @RequestMapping(value = {"/domains"}, method = { RequestMethod.GET })
    @ResponseBody
    public List<DomainDto> getDomains() throws Exception {
        if (!isAuthorized(AuthObjects.Permissions.ViewExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

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
                        .laws(Stream.concat(d.getPositiveLaws().stream(), d.getNegativeLaws().stream())
                                .filter(x -> x.hasFlag(Law.FLAG_VISIBLE_TO_TEACHER))
                                .map(x -> new LawTreeItemDto(x.getName(), d.getLawDisplayName(x.getName(), currentLanguage), x.getBitflags()))
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
