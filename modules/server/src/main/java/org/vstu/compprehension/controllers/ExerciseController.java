package org.vstu.compprehension.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.Service.ExerciseAttemptService;
import org.vstu.compprehension.Service.FrontendService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.ExerciseAttemptDto;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.dto.ExerciseInfoDto;
import org.vstu.compprehension.dto.ExerciseStatisticsItemDto;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.repository.ExerciseRepository;

import java.util.List;

@Controller
@RequestMapping("api/exercise")
@Log4j2
public class ExerciseController {
    private final FrontendService frontendService;
    private final UserService userService;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseAttemptService exerciseAttemptService;

    @Autowired
    public ExerciseController(FrontendService frontendService, UserService userService,
                              ExerciseRepository exerciseRepository, ExerciseAttemptService exerciseAttemptService) {
        this.frontendService = frontendService;
        this.userService = userService;
        this.exerciseRepository = exerciseRepository;
        this.exerciseAttemptService = exerciseAttemptService;
    }

    /**
     * Returns all exercises
     * @return List of exercises
     */
    @RequestMapping(value = { "all"}, method = { RequestMethod.GET })
    @ResponseBody
    public List<ExerciseDto> getAll() {
        return exerciseRepository.getAllExerciseItems();
    }

    /**
     * Returns exercise by id
     * @param id Exercise id
     * @return Exercise
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"shortInfo"}, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseInfoDto getExerciseShortInfo(long id, HttpServletRequest request) throws Exception {
        ExerciseEntity exercise = exerciseRepository.findById(id).orElseThrow();
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
    public ExerciseStatisticsItemDto[] getExerciseStatistics(Long exerciseId) {
        return frontendService.getExerciseStatistics(exerciseId);
    }

    @RequestMapping(value = {"getExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    public @NotNull ExerciseAttemptDto getExerciseAttempt(Long attemptId, HttpServletRequest request) throws Exception {
        Long userId = userService.getCurrentUser().getId();
        ExerciseAttemptDto result = frontendService.getExerciseAttempt(attemptId);
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
        Long userId = userService.getCurrentUser().getId();
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
        Long userId = userService.getCurrentUser().getId();
        ExerciseAttemptDto attempt = frontendService.createExerciseAttempt(exerciseId, userId);

        // If launched via LTI, attach the AGS context for grade passback
        String lineitemUrl = (String) request.getSession().getAttribute("ltiLineitemUrl");
        if (lineitemUrl != null) {
            String contextId = (String) request.getSession().getAttribute("ltiContextId");
            String ltiUserId = (String) request.getSession().getAttribute("ltiUserId");
            exerciseAttemptService.setLtiContext(attempt.getAttemptId(), lineitemUrl, contextId, ltiUserId);
        }
        return attempt;
    }

    @RequestMapping(value = {"createDebugExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseAttemptDto createDebugExerciseAttempt(Long exerciseId, HttpServletRequest request) throws Exception {
        Long userId = userService.getCurrentUser().getId();
        return frontendService.createSolvedExerciseAttempt(exerciseId, userId);
    }
}
