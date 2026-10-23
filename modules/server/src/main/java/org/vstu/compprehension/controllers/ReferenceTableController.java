package org.vstu.compprehension.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.ReferenceTableFrontendService;
import org.vstu.compprehension.frontend.UserFrontendService;
import org.vstu.compprehension.frontend.dto.DomainDto;
import org.vstu.compprehension.frontend.dto.StrategyDto;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping({"api/refTables" })
@RequiredArgsConstructor
public class ReferenceTableController {
    private final ReferenceTableFrontendService referenceDataService;
    private final UserFrontendService userService;

    private Language currentLanguage() {
        return userService.tryGetCurrentUserLanguage().orElse(Language.ENGLISH);
    }

    @RequestMapping(value = {"/strategies"}, method = { RequestMethod.GET })
    @ResponseBody
    public List<StrategyDto> getStrategies() {
        return referenceDataService.getStrategies(currentLanguage());
    }

    @RequestMapping(value = {"/backends"}, method = { RequestMethod.GET })
    @ResponseBody
    public Set<String> getBackends() {
        return referenceDataService.getBackendIds();
    }

    @RequestMapping(value = {"/domains"}, method = { RequestMethod.GET })
    @ResponseBody
    public List<DomainDto> getDomains() {
        return referenceDataService.getDomains(currentLanguage());
    }
}
