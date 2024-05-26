package org.vstu.compprehension.controllers;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.AuthorizationService;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.survey.SurveyDto;
import org.vstu.compprehension.dto.survey.SurveyResultDto;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects;
import org.vstu.compprehension.models.entities.SurveyAnswerEntity;
import org.vstu.compprehension.models.repository.QuestionRepository;
import org.vstu.compprehension.models.repository.SurveyAnswerRepository;
import org.vstu.compprehension.models.repository.SurveyRepository;
import org.vstu.compprehension.utils.Mapper;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("api/survey")
@Log4j2
public class QuestionSurveyController {
    private final UserService userService;
    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final SurveyAnswerRepository surveyResultRepository;
    private final CourseService courseService;
    private final AuthorizationService authorizationService;

    @Autowired
    public QuestionSurveyController(UserService userService, SurveyRepository surveyRepository, QuestionRepository questionRepository, SurveyAnswerRepository surveyResultRepository, CourseService courseService, AuthorizationService authorizationService) {
        this.userService = userService;
        this.surveyRepository = surveyRepository;
        this.questionRepository = questionRepository;
        this.surveyResultRepository = surveyResultRepository;
        this.courseService = courseService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/{id}")
    @ResponseBody
    public SurveyDto getSurvey(@PathVariable("id") String surveyId) throws Exception {
        if (!isAuthorized(AuthObjects.Permissions.ViewExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        var survey = surveyRepository.findOne(surveyId)
                .orElseThrow(() -> new IllegalStateException(String.format("Couldn't find survey with id %s", surveyId)));
        return Mapper.toDto(survey);
    }

    private boolean isAuthorized(String permissionName) throws Exception {
        var course = courseService.getCurrentCourse();
        var userId = userService.getCurrentUser().getId();

        return authorizationService.isAuthorizedAnyCourseOrGlobal(
                userId,
                permissionName,
                Optional.ofNullable(course != null ? course.getId() : null)
        );
    }

    @SneakyThrows
    @GetMapping("/{id}/user-votes")
    @ResponseBody
    public List<SurveyResultDto> getCurrentUserAttemptSurveyResults(@PathVariable("id") String surveyId,
                                                                    @RequestParam("attemptId") Long attemptId) {
        if (!isAuthorized(AuthObjects.Permissions.ViewExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        var userId = userService.getCurrentUser().getId();
        return surveyRepository.findUserAttemptVotes(userId, attemptId, surveyId);
    }

    @PostMapping("")
    public ResponseEntity<?> addSurveyResult(@RequestBody SurveyResultDto result) throws Exception {
        if (!isAuthorized(AuthObjects.Permissions.ViewExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        var currentUser = userService.getCurrentUser();

        // ensure question correct
        var question = questionRepository.findById(result.getQuestionId())
                .orElseThrow(() -> new IllegalStateException("Couldn't find question"));

        // ensure user correct
        var questionUser = question.getExerciseAttempt().getUser();
        if (!questionUser.getId().equals(currentUser.getId()))
            throw new IllegalStateException("Invalid user");

        // ensure surveyQuestion correct
        var surveyQuestion = surveyRepository.findSurveyQuestion(result.getSurveyQuestionId())
                .orElseThrow(() -> new IllegalStateException("Invalid survey question "));

        var id = new SurveyAnswerEntity.SurveyResultId(result.getSurveyQuestionId(), question.getId(), currentUser.getId());
        var existingEntity = surveyResultRepository.findById(id).orElse(null);
        if (existingEntity == null)
            existingEntity = new SurveyAnswerEntity();
        existingEntity.setQuestion(question);
        existingEntity.setSurveyQuestion(surveyQuestion);
        existingEntity.setUser(questionUser);
        existingEntity.setResult(result.getAnswer());
        surveyResultRepository.save(existingEntity);
        return ResponseEntity.ok().build();
    }
}
