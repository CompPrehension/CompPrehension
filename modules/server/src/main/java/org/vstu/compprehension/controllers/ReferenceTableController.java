package org.vstu.compprehension.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.models.businesslogic.Concept;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping({"basic/refTables", "lti/refTables" })
public class ReferenceTableController {
    private final DomainFactory domainFactory;

    @Autowired
    public ReferenceTableController(DomainFactory domainFactory) {
        this.domainFactory = domainFactory;
    }

    @RequestMapping(value = {"/domains"}, method = { RequestMethod.GET })
    @ResponseBody
    public Set<String> getDomains() {
        return domainFactory.getDomainIds();
    }

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
}
