package org.vstu.compprehension.controllers.interfaces;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.dto.*;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.dto.question.QuestionDto;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface ExerciseController {

    @RequestMapping(value = {"/getExercises"}, method = { RequestMethod.GET })
    @ResponseBody
    List<Long> getExercises(HttpServletRequest request) throws Exception;

    /**
     * Get existing exercise attempt for current user
     * @param exerciseId Exercise id
     * @param request Current request
     * @return Existing exercise attempt or null
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"/getExistingExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    ExerciseAttemptDto getExistingExerciseAttempt(Long exerciseId, HttpServletRequest request) throws Exception;

    /**
     * Create exercise attempt for current user
     * @param exerciseId Exercise id
     * @param request Current request
     * @return Created attempt
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"/createExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    ExerciseAttemptDto createExerciseAttempt(Long exerciseId, HttpServletRequest request) throws Exception;

    /**
     * Returns statistics for all user's exercise attempt
     * @param exerciseId Exercise id
     * @return Statistics
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"/getExerciseStatistics"}, method = { RequestMethod.GET })
    @ResponseBody
    ExerciseStatisticsItemDto[] getExerciseStatistics(Long exerciseId) throws Exception;

    /**
     * Add an answer to the question
     * @param interaction Interaction object
     * @param request Current request
     * @return Feedback
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"/addQuestionAnswer"}, method = { RequestMethod.POST }, produces = "application/json",
            consumes = "application/json")
    @ResponseBody
    FeedbackDto addQuestionAnswer(@RequestBody InteractionDto interaction, HttpServletRequest request) throws Exception;

    /**
     * Generate new question for exercise attempt
     * @param exAttemptId Exercise attempt id
     * @param request Current request
     * @return Question
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"/generateQuestion"}, method = { RequestMethod.GET })
    @ResponseBody
    QuestionDto generateQuestion(@RequestParam(name = "attemptId") Long exAttemptId, HttpServletRequest request) throws Exception;

    /**
     * Generate new supplementary question
     * @param questionRequest QuestionRequest
     * @param request Current request
     * @return Question
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"/generateSupplementaryQuestion"}, method = { RequestMethod.POST })
    @ResponseBody
    QuestionDto generateSupplementaryQuestion(@RequestBody SupplementaryQuestionRequestDto questionRequest, HttpServletRequest request) throws Exception;

    /**
     * Get question by id
     * @param questionId Question Id
     * @param request Current request
     * @return Question
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"/getQuestion"}, method = { RequestMethod.GET })
    @ResponseBody
    QuestionDto getQuestion(@RequestParam Long questionId, HttpServletRequest request) throws Exception;

    /**
     * Generate next correct answer
     * @param questionId Question Id
     * @param request Current request
     * @return Next correct answer
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"/generateNextCorrectAnswer"}, method = { RequestMethod.GET })
    @ResponseBody
    FeedbackDto generateNextCorrectAnswer(@RequestParam Long questionId, HttpServletRequest request) throws Exception;

    /**
     * Root method for launching an exercise
     * @param exerciseId Exercise id
     * @param request Current request
     * @return Html page
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"/pages/exercise"}, method = { RequestMethod.GET })
    String launch(Model model, Long exerciseId, HttpServletRequest request);

    /**
     * Load session info
     * @param request Current request
     * @return Session info
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"/loadSessionInfo"}, method = { RequestMethod.GET })
    @ResponseBody
    SessionInfoDto loadSessionInfo(HttpServletRequest request) throws Exception;

    /**
     * Get current user info
     * @param request Current request
     * @return User info
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"/getCurrentUser"}, method = { RequestMethod.GET })
    @ResponseBody
    UserInfoDto getCurrentUser(HttpServletRequest request) throws Exception;
}
