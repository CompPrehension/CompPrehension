package org.vstu.compprehension.controllers;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.SurveyService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.survey.SurveyDto;
import org.vstu.compprehension.dto.survey.SurveyResultDto;

import java.util.List;

@Controller
@RequestMapping("api/survey")
@Log4j2
public class QuestionSurveyController {
    private final UserService userService;
    private final SurveyService surveyService;

    @Autowired
    public QuestionSurveyController(UserService userService, SurveyService surveyService) {
        this.userService = userService;
        this.surveyService = surveyService;
    }

    @GetMapping("/{id}")
    @ResponseBody
    public SurveyDto getSurvey(@PathVariable("id") String surveyId) {
        return surveyService.getSurvey(surveyId);
    }

    @SneakyThrows
    @GetMapping("/{id}/user-votes")
    @ResponseBody
    public List<SurveyResultDto> getCurrentUserAttemptSurveyResults(@PathVariable("id") String surveyId,
                                                                    @RequestParam("attemptId") Long attemptId) {
        var userId = userService.getCurrentUser().id();
        return surveyService.getUserAttemptVotes(userId, attemptId, surveyId);
    }

    @PostMapping("")
    public ResponseEntity<?> addSurveyResult(@RequestBody SurveyResultDto result) throws Exception {
        var userId = userService.getCurrentUser().id();
        surveyService.saveAnswer(userId, result);
        return ResponseEntity.ok().build();
    }
}
