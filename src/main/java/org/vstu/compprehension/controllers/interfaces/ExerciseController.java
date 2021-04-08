package org.vstu.compprehension.controllers.interfaces;

import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.dto.*;
import org.vstu.compprehension.dto.question.QuestionDto;

import javax.servlet.http.HttpServletRequest;

public interface ExerciseController {
    @RequestMapping(value = {"/getExistingExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    ExerciseAttemptDto getExistingExerciseAttempt(Long exerciseId, HttpServletRequest request);

    @RequestMapping(value = {"/createExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    ExerciseAttemptDto createExerciseAttempt(Long exerciseId, HttpServletRequest request);

    @RequestMapping(value = {"/getExerciseStatistics"}, method = { RequestMethod.GET })
    @ResponseBody
    ExerciseStatisticsItemDto[] getExerciseStatistics(Long exerciseId);

    @RequestMapping(value = {"/addQuestionAnswer"}, method = { RequestMethod.POST }, produces = "application/json",
            consumes = "application/json")
    @ResponseBody
    FeedbackDto addQuestionAnswer(@RequestBody InteractionDto interaction, HttpServletRequest request) throws Exception;

    @RequestMapping(value = {"/generateQuestion"}, method = { RequestMethod.GET })
    @ResponseBody
    QuestionDto generateQuestion(@RequestParam(name = "attemptId") Long exAttemptId, HttpServletRequest request) throws Exception;

    @RequestMapping(value = {"/getQuestion"}, method = { RequestMethod.GET })
    @ResponseBody
    QuestionDto getQuestion(@RequestParam Long questionId, HttpServletRequest request) throws Exception;

    @RequestMapping(value = {"/"}, method = { RequestMethod.GET })
    String launch(Long exerciseId, HttpServletRequest request) throws Exception;

    @RequestMapping(value = {"/pages/**"}, method = { RequestMethod.GET })
    String pages(HttpServletRequest request);

    @RequestMapping(value = {"/loadSessionInfo"}, method = { RequestMethod.GET })
    @ResponseBody
    SessionInfoDto loadSessionInfo(HttpServletRequest request) throws Exception;
}
