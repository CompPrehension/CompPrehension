package org.vstu.compprehension.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.frontend.AuthFrontendService;
import org.vstu.compprehension.frontend.QuestionBankSearchFrontendService;
import org.vstu.compprehension.frontend.UserFrontendService;
import org.vstu.compprehension.frontend.dto.QuestionBankSearchRequestDto;
import org.vstu.compprehension.frontend.dto.QuestionBankSearchStatsDto;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;

@Controller
@RequestMapping("api/question-bank")
@Log4j2
@RequiredArgsConstructor
public class QuestionBankController {
    private final QuestionBankSearchFrontendService questionBankSearchService;
    private final UserFrontendService userService;
    private final AuthFrontendService authService;

    @RequestMapping(value = {"search"}, method = { RequestMethod.POST }, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public QuestionBankSearchStatsDto search(@RequestBody QuestionBankSearchRequestDto searchRequest,
                                             HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUserId();
        authService.ensureAuthorized(userId, SystemPermission.VIEW_EXERCISE, authService.courseOrGlobal(searchRequest.getCourseId()));
        return questionBankSearchService.search(searchRequest);
    }
}
