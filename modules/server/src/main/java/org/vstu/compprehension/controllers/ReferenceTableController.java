package org.vstu.compprehension.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.dto.DomainDto;
import org.vstu.compprehension.models.businesslogic.Concept;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.backend.BackendFactory;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.strategies.StrategyFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping({"basic/refTables", "lti/refTables" })
public class ReferenceTableController {
    private final DomainFactory domainFactory;
    private final StrategyFactory strategyFactory;
    private final BackendFactory backendFactory;

    @Autowired
    public ReferenceTableController(DomainFactory domainFactory, StrategyFactory strategyFactory, BackendFactory backendFactory) {
        this.domainFactory = domainFactory;
        this.strategyFactory = strategyFactory;
        this.backendFactory = backendFactory;
    }

    @RequestMapping(value = {"/strategies"}, method = { RequestMethod.GET })
    @ResponseBody
    public Set<String> getStrategies() {
        return strategyFactory.getStrategyIds();
    }

    @RequestMapping(value = {"/backends"}, method = { RequestMethod.GET })
    @ResponseBody
    public Set<String> getBackends() {
        return backendFactory.getBackendIds();
    }

    @RequestMapping(value = {"/domains"}, method = { RequestMethod.GET })
    @ResponseBody
    public List<DomainDto> getDomains() {
        var domainIds= domainFactory.getDomainIds();
        // var result = new ArrayList<DomainDto>(domainIds.size());

        // for (var domainId : domainIds) {
        //    var domain = domainFactory.getDomain(domainId);
        //    var rawConceptsTree = domain.getConceptsSimplifiedHierarchy(Concept.FLAG_VISIBLE_TO_TEACHER);


            //var conceptToParentMap = new HashMap<Concept, Concept>();
            //for (var rawConcept: rawConceptsTree.entrySet()) {
            //    for (var rawConceptChild: rawConcept.getValue()) {
            //        conceptToParentMap.put(rawConceptChild, rawConcept.getKey());
            //    }
            //}
        //}

        //return result;

        return domainIds.stream()
                .map(domainFactory::getDomain)
                .map(d -> DomainDto.builder()
                        .id(d.getDomainId())
                        .name(d.getName())
                        .concepts(d.getConcepts())
                        .concepts2(d.getConceptsSimplifiedHierarchy(Concept.FLAG_VISIBLE_TO_TEACHER))
                        .laws(Stream.concat(d.getPositiveLaws().stream(), d.getNegativeLaws().stream())
                                .filter(x -> x.hasFlag(Law.FLAG_VISIBLE_TO_TEACHER))
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
