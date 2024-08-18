package org.vstu.compprehension.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.Service.AuthorizationService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.QuestionBankCountDto;
import org.vstu.compprehension.dto.QuestionBankSearchRequestDto;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.entities.EnumData.Role;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;

import java.util.Objects;

@Controller
@RequestMapping("api/question-bank")
@Log4j2
public class QuestionBankController {
    private final DomainFactory domainFactory;
    private final QuestionBank questionStorage;
    private final UserService userService;
    private final AuthorizationService authorizationService;

    @Autowired
    public QuestionBankController(DomainFactory domainFactory, QuestionBank questionStorage, UserService userService, AuthorizationService authorizationService) {
        this.domainFactory   = domainFactory;
        this.questionStorage = questionStorage;
        this.userService     = userService;
        this.authorizationService = authorizationService;
    }

    @RequestMapping(value = {"count"}, method = { RequestMethod.POST }, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public QuestionBankCountDto getQuestionsCount(@RequestBody QuestionBankSearchRequestDto searchRequest, HttpServletRequest request) throws Exception {
        var currentUser = userService.getCurrentUser();
        var courceId = 1;
        if (!authorizationService.isAuthorizedCourse(currentUser.getId(), AuthObjects.Permissions.ViewExercise.Name(), courceId)) {
            throw new AuthorizationServiceException("Authorization error");
        }

        var domain = domainFactory.getDomain(searchRequest.getDomainId());

        var targetConcepts = searchRequest.getConcepts().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                .flatMap(c -> domain.getConceptWithChildren(c.getName()).stream())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        var deniedConcepts = searchRequest.getConcepts().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                .flatMap(c -> domain.getConceptWithChildren(c.getName()).stream())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        var targetLaws = searchRequest.getLaws().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                .map(c -> domain.getLaw(c.getName()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        var deniedLaws = searchRequest.getLaws().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                .map(c -> domain.getLaw(c.getName()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        var targetTags = searchRequest.getTags().stream()
                .map(domain::getTag)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        var qr = QuestionRequest.builder()
                .targetConcepts(targetConcepts)
                .deniedConcepts(deniedConcepts)
                .targetLaws(targetLaws)
                .deniedLaws(deniedLaws)
                .complexity(searchRequest.getComplexity())
                .targetTags(targetTags)
                .domainShortname(domain.getShortnameForQuestionSearch())
                .build();
        qr = domain.ensureQuestionRequestValid(qr);
        return questionStorage.countQuestionsWithTopRated(qr);
    }
}
