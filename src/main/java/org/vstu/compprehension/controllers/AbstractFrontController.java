package org.vstu.compprehension.controllers;

import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.dto.ExerciseStatisticsItemDto;
import org.vstu.compprehension.dto.FeedbackDto;
import org.vstu.compprehension.dto.InteractionDto;
import org.vstu.compprehension.dto.SessionInfoDto;
import org.vstu.compprehension.dto.question.QuestionDto;

import javax.servlet.http.HttpServletRequest;

public interface AbstractFrontController {
    @RequestMapping(value = {"/"}, method = { RequestMethod.GET })
    String launch(HttpServletRequest request) throws Exception;

    @RequestMapping(value = {"/pages/**"}, method = { RequestMethod.GET })
    String pages(HttpServletRequest request);

    @RequestMapping(value = {"/addAnswer"}, method = { RequestMethod.POST }, produces = "application/json",
            consumes = "application/json")
    @ResponseBody
    FeedbackDto addAnswer(@RequestBody InteractionDto interaction, HttpServletRequest request) throws Exception;

    @RequestMapping(value = {"/generateQuestion"}, method = { RequestMethod.GET })
    @ResponseBody
    QuestionDto generateQuestion(@RequestParam(name = "attemptId") Long exAttemptId) throws Exception;

    @RequestMapping(value = {"/getQuestion"}, method = { RequestMethod.GET })
    @ResponseBody
    QuestionDto getQuestion(@RequestParam Long questionId) throws Exception;

    @RequestMapping(value = {"/loadSessionInfo"}, method = { RequestMethod.GET })
    @ResponseBody
    SessionInfoDto loadSessionInfo(HttpServletRequest request) throws Exception;

    @RequestMapping(value = {"/getExerciseStatistics"}, method = { RequestMethod.GET })
    @ResponseBody
    ExerciseStatisticsItemDto[] getExerciseStatistics(Long exerciseId);
}
