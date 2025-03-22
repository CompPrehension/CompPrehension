package org.vstu.compprehension.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.FrontendService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.InteractionDto;
import org.vstu.compprehension.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.dto.SupplementaryQuestionDto;
import org.vstu.compprehension.dto.SupplementaryQuestionRequestDto;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.entities.EnumData.Role;

@Controller
@RequestMapping("api/question")
@Log4j2
public class QuestionController {
    private final FrontendService frontendService;
    private final UserService userService;

    public QuestionController(FrontendService frontendService, UserService userService) {
        this.frontendService = frontendService;
        this.userService = userService;
    }

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
        return frontendService.addQuestionAnswer(interaction);
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
        return frontendService.addSupplementaryQuestionAnswer(interaction);
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
        val locale = LocaleContextHolder.getLocale();
        return frontendService.generateQuestion(attemptId);
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
        var currentUser = userService.getCurrentUser();
        if (!currentUser.getRoles().contains(Role.TEACHER)) {
            throw new AuthorizationServiceException("Unathorized");
        }

        return frontendService.generateQuestionByMetadata(metadataId, currentUser.getPreferred_language());
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
        val locale = LocaleContextHolder.getLocale();
        return frontendService.generateSupplementaryQuestion(questionRequest.getQuestionId(), questionRequest.getViolationLaws());
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
        return frontendService.getQuestion(questionId);
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
        val locale = LocaleContextHolder.getLocale();
        return frontendService.generateNextCorrectAnswer(questionId);
    }
}
