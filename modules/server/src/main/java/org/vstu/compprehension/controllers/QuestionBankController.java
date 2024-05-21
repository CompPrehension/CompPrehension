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
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.QuestionBankSearchRequestDto;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.EnumData.Role;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;

import java.util.Optional;

@Controller
@RequestMapping("api/question-bank")
@Log4j2
public class QuestionBankController {
    private final DomainFactory domainFactory;
    private final QuestionBank questionStorage;
    private final UserService userService;
    private final CourseService courseService;
    private final AuthorizationService authorizationService;

    @Autowired
    public QuestionBankController(DomainFactory domainFactory, QuestionBank questionStorage, UserService userService, CourseService courseService, AuthorizationService authorizationService) {
        this.domainFactory   = domainFactory;
        this.questionStorage = questionStorage;
        this.userService     = userService;
        this.courseService = courseService;
        this.authorizationService = authorizationService;
    }

    @RequestMapping(value = {"count"}, method = { RequestMethod.POST }, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public Integer getQuestionsCount(@RequestBody QuestionBankSearchRequestDto searchRequest, HttpServletRequest request) throws Exception {
        var initCourse = courseService.getInitialCourseId();
        var courseId = searchRequest.getCourseId() == null ? initCourse : Long.parseLong(searchRequest.getCourseId());

        var currentUser = userService.getCurrentUser();

        var isAuthorized = authorizationService.isAuthorized(
                    currentUser.getId(),
                    AuthObjects.Permissions.editExercise.name(),
                    PermissionScopeKind.COURSE,
                    Optional.of(courseId))
                ||
                authorizationService.isAuthorized(
                    currentUser.getId(),
                    AuthObjects.Permissions.editExercise.name(),
                    PermissionScopeKind.GLOBAL,
                    Optional.empty());

        if (!isAuthorized) {
            throw new AuthorizationServiceException("Unathorized");
        }
        
        var domain = domainFactory.getDomain(searchRequest.getDomainId());

        var targetConcepts = searchRequest.getConcepts().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                .flatMap(c -> domain.getConceptWithChildren(c.getName()).stream())
                .distinct()
                .toList();
        var deniedConcepts = searchRequest.getConcepts().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                .flatMap(c -> domain.getConceptWithChildren(c.getName()).stream())
                .distinct()
                .toList();
        var targetLaws = searchRequest.getLaws().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                .map(c -> domain.getLaw(c.getName()))
                .distinct()
                .toList();
        var deniedLaws = searchRequest.getLaws().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                .map(c -> domain.getLaw(c.getName()))
                .distinct()
                .toList();
        var targetTags = searchRequest.getTags().stream()
                .map(domain::getTag)
                .distinct()
                .toList();

        var qr = QuestionRequest.builder()
                .targetConcepts(targetConcepts)
                .deniedConcepts(deniedConcepts)
                .targetLaws(targetLaws)
                .deniedLaws(deniedLaws)
                .complexity(searchRequest.getComplexity())
                .targetTags(targetTags)
                .domainShortname(domain.getShortName())
                .build();
        qr = domain.ensureQuestionRequestValid(qr);
        return questionStorage.countQuestions(qr);
    }
}
