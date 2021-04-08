package org.vstu.compprehension.controllers.interfaces;

import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.dto.FeedbackDto;
import org.vstu.compprehension.dto.InteractionDto;
import org.vstu.compprehension.dto.question.QuestionDto;

import javax.servlet.http.HttpServletRequest;

public interface QuestionController {
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
}
