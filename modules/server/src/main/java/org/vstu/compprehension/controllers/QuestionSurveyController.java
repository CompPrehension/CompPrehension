package org.vstu.compprehension.controllers;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.frontend.SurveyFrontendService;
import org.vstu.compprehension.frontend.UserFrontendService;
import org.vstu.compprehension.frontend.dto.survey.SurveyDto;
import org.vstu.compprehension.frontend.dto.survey.SurveyResultDto;

import java.util.List;

@Controller
@RequestMapping("api/survey")
@Log4j2
@RequiredArgsConstructor
public class QuestionSurveyController {
    private final UserFrontendService userFacade;
    private final SurveyFrontendService surveyService;

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
        var userId = userFacade.getCurrentUserId();
        return surveyService.getUserAttemptVotes(userId, attemptId, surveyId);
    }

    @PostMapping("")
    public ResponseEntity<?> addSurveyResult(@RequestBody SurveyResultDto result) throws Exception {
        var userId = userFacade.getCurrentUserId();
        surveyService.saveAnswer(userId, result);
        return ResponseEntity.ok().build();
    }
}
