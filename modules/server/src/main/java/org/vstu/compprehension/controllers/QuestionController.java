package org.vstu.compprehension.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.frontend.AuthFrontendService;
import org.vstu.compprehension.frontend.ExerciseAttemptFrontendService;
import org.vstu.compprehension.frontend.UserFrontendService;
import org.vstu.compprehension.frontend.dto.InteractionDto;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.frontend.dto.SupplementaryQuestionDto;
import org.vstu.compprehension.frontend.dto.SupplementaryQuestionRequestDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.frontend.dto.question.QuestionDto;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;

@Controller
@RequestMapping("api/question")
@Log4j2
@RequiredArgsConstructor
public class QuestionController {
    private final ExerciseAttemptFrontendService exerciseAttemptService;
    private final UserFrontendService userService;
    private final AuthFrontendService authService;

    /**
     * Add an answer to the question
     * @param interaction Interaction object
     * @param request Current request
     * @return Feedback
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"addQuestionAnswer"}, method = { RequestMethod.POST }, produces = "application/json",
            consumes = "application/json")
    @ResponseBody
    public FeedbackDto addQuestionAnswer(@RequestBody InteractionDto interaction, HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUserId();
        exerciseAttemptService.ensureCanAccessQuestion(userId, interaction.getQuestionId());
        return exerciseAttemptService.addQuestionAnswer(interaction);
    }

    /**
     * Add an answer to the question
     * @param interaction Interaction object
     * @param request Current request
     * @return Feedback
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"addSupplementaryQuestionAnswer"}, method = { RequestMethod.POST }, produces = "application/json",
            consumes = "application/json")
    @ResponseBody
    public SupplementaryFeedbackDto addSupplementaryQuestionAnswer(@RequestBody InteractionDto interaction, HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUserId();
        exerciseAttemptService.ensureCanAccessQuestion(userId, interaction.getQuestionId());
        return exerciseAttemptService.addSupplementaryQuestionAnswer(interaction);
    }

    /**
     * Generate new question for exercise attempt
     * @param attemptId Exercise attempt id
     * @param request Current request
     * @return Question
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"generate"}, method = { RequestMethod.GET })
    @ResponseBody
    public QuestionDto generateQuestion(Long attemptId, HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUserId();
        exerciseAttemptService.ensureCanAccessAttempt(userId, attemptId);
        return exerciseAttemptService.generateQuestion(attemptId);
    }

    /**
     * Generate new question by metadata
     * @param metadataId Exercise attempt id
     * @param request Current request
     * @return Question
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"generateByMetadata"}, method = { RequestMethod.GET })
    @ResponseBody
    public QuestionDto generateQuestionByMetadata(Integer metadataId, HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUserId();
        authService.ensureAuthorized(userId, SystemPermission.EDIT_EXERCISE, authService.global());

        return exerciseAttemptService.generateQuestionByMetadata(metadataId, userService.getCurrentUserLanguage());
    }

    /**
     * Generate new supplementary question
     * @param questionRequest QuestionRequest
     * @param request Current request
     * @return Question
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"generateSupplementaryQuestion"}, method = { RequestMethod.POST })
    @ResponseBody
    public SupplementaryQuestionDto generateSupplementaryQuestion(@RequestBody SupplementaryQuestionRequestDto questionRequest, HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUserId();
        exerciseAttemptService.ensureCanAccessQuestion(userId, questionRequest.getQuestionId());
        return exerciseAttemptService.generateSupplementaryQuestion(questionRequest.getQuestionId(), questionRequest.getViolationLaws());
    }

    /**
     * Get question by id
     * @param questionId Question Id
     * @param request Current request
     * @return Question
     * @throws Exception Something got wrong
     */
    @RequestMapping(method = { RequestMethod.GET })
    @ResponseBody
    public QuestionDto getQuestion(Long questionId, HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUserId();
        exerciseAttemptService.ensureCanAccessQuestion(userId, questionId);
        return exerciseAttemptService.getQuestion(questionId);
    }

    /**
     * Generate next correct answer
     * @param questionId Question Id
     * @param request Current request
     * @return Next correct answer
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"generateNextCorrectAnswer"}, method = { RequestMethod.GET })
    @ResponseBody
    public FeedbackDto generateNextCorrectAnswer(@RequestParam Long questionId, HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUserId();
        exerciseAttemptService.ensureCanAccessQuestion(userId, questionId);
        return exerciseAttemptService.generateNextCorrectAnswer(questionId);
    }
}
