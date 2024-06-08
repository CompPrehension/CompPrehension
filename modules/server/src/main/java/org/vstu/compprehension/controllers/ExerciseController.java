package org.vstu.compprehension.controllers;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.AuthorizationService;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.FrontendService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.*;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects;
import org.vstu.compprehension.models.repository.ExerciseRepository;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("api/exercise")
@Log4j2
public class ExerciseController {
    private final FrontendService frontendService;
    private final UserService userService;
    private final ExerciseRepository exerciseRepository;
    private final CourseService courseService;
    private final AuthorizationService authorizationService;

    @Autowired
    public ExerciseController(FrontendService frontendService, UserService userService, ExerciseRepository exerciseRepository, CourseService courseService, AuthorizationService authorizationService) {
        this.frontendService = frontendService;
        this.userService = userService;
        this.exerciseRepository = exerciseRepository;
        this.courseService = courseService;
        this.authorizationService = authorizationService;
    }

    @SneakyThrows
    @RequestMapping(value = { "all"}, method = { RequestMethod.GET })
    @ResponseBody
    public List<ExerciseDto> getAll() {
        if (!isAuthorized(AuthObjects.Permissions.ViewExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        var course = courseService.getCurrentCourse();
        return exerciseRepository.getAllExerciseItemsByCourseId(course.getId());
    }

    private boolean isAuthorized(String permissionName) throws Exception {
        var course = courseService.getCurrentCourse();
        var userId = userService.getCurrentUser().getId();

        return authorizationService.isAuthorizedCourse(
                userId,
                permissionName,
                course.getId()
        );
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
        if (!isAuthorized(AuthObjects.Permissions.SolveExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

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
        if (!isAuthorized(AuthObjects.Permissions.SolveExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        return frontendService.addSupplementaryQuestionAnswer(interaction);
    }

    /**
     * Generate new question for exercise attempt
     * @param attemptId Exercise attempt id
     * @param request Current request
     * @return Question
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"generateQuestion"}, method = { RequestMethod.GET })
    @ResponseBody
    public QuestionDto generateQuestion(Long attemptId, HttpServletRequest request) throws Exception {
        if (!isAuthorized(AuthObjects.Permissions.SolveExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        val locale = LocaleContextHolder.getLocale();
        return frontendService.generateQuestion(attemptId);
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
        if (!isAuthorized(AuthObjects.Permissions.CreateExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

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
    @RequestMapping(value = {"getQuestion"}, method = { RequestMethod.GET })
    @ResponseBody
    public QuestionDto getQuestion(Long questionId, HttpServletRequest request) throws Exception {
        if (!isAuthorized(AuthObjects.Permissions.ViewExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

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
        if (!isAuthorized(AuthObjects.Permissions.ViewExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        val locale = LocaleContextHolder.getLocale();;
        return frontendService.generateNextCorrectAnswer(questionId);
    }


    /**
     * Load session info
     * @param request Current request
     * @return Session info
     * @throws Exception Something got wrong
     */


    @RequestMapping(value = {"shortInfo"}, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseInfoDto getExerciseShortInfo(long id, HttpServletRequest request) throws Exception {
        if (!isAuthorized(AuthObjects.Permissions.ViewExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        val exercise = exerciseRepository.findById(id).orElseThrow();
        return new ExerciseInfoDto(id, exercise.getOptions());
    }

    /**
     * Returns statistics for all user's exercise attempt
     * @param exerciseId Exercise id
     * @return Statistics
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"getExerciseStatistics"}, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseStatisticsItemDto[] getExerciseStatistics(Long exerciseId) throws Exception {
        if (!isAuthorized(AuthObjects.Permissions.ViewExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        return frontendService.getExerciseStatistics(exerciseId);
    }

    @RequestMapping(value = {"getExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    public @NotNull ExerciseAttemptDto getExerciseAttempt(Long attemptId, HttpServletRequest request) throws Exception {
        if (!isAuthorized(AuthObjects.Permissions.SolveExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        val userId = userService.getCurrentUser().getId();
        var result = frontendService.getExerciseAttempt(attemptId);
        if (result == null) {
            throw new Exception("No such attempt");
        }
        if (!userId.equals(result.getUserId())){
            throw new AuthorizationServiceException("Authorization error");
        }
        return result;
    }

    /**
     * Get existing exercise attempt for current user
     * @param exerciseId Exercise id
     * @param request Current request
     * @return Existing exercise attempt or null
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"getExistingExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseAttemptDto getExistingExerciseAttempt(Long exerciseId, HttpServletRequest request) throws Exception {
        if (!isAuthorized(AuthObjects.Permissions.SolveExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        val userId = userService.getCurrentUser().getId();
        return frontendService.getExistingExerciseAttempt(exerciseId, userId);
    }

    /**
     * Create exercise attempt for current user
     * @param exerciseId Exercise id
     * @param request Current request
     * @return Created attempt
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"createExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseAttemptDto createExerciseAttempt(Long exerciseId, HttpServletRequest request) throws Exception {
        if (!isAuthorized(AuthObjects.Permissions.SolveExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        val userId = userService.getCurrentUser().getId();
        return frontendService.createExerciseAttempt(exerciseId, userId);
    }

    @RequestMapping(value = {"createDebugExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseAttemptDto createDebugExerciseAttempt(Long exerciseId, HttpServletRequest request) throws Exception {
        if (!isAuthorized(AuthObjects.Permissions.SolveExercise.Name())) {
            throw new AuthorizationServiceException("Unathorized");
        }

        val userId = userService.getCurrentUser().getId();
        return frontendService.createSolvedExerciseAttempt(exerciseId, userId);
    }
}
