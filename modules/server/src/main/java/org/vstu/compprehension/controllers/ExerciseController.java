package org.vstu.compprehension.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.Service.AuthScopeFactory;
import org.vstu.compprehension.Service.AuthService;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.ExerciseAttemptService;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.FrontendService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.ExerciseAttemptDto;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.dto.ExerciseInfoDto;
import org.vstu.compprehension.dto.ExerciseStatisticsItemDto;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.SystemPermission;

import java.util.List;

@Controller
@RequestMapping("api/exercise")
@Log4j2
public class ExerciseController {
    private final FrontendService frontendService;
    private final CourseService courseService;
    private final UserService userService;
    private final ExerciseService exerciseService;
    private final AuthService authService;
    private final AuthScopeFactory authScopes;
    private final ExerciseAttemptService exerciseAttemptService;

    @Autowired
    public ExerciseController(FrontendService frontendService, CourseService courseService, UserService userService,
                              ExerciseService exerciseService, AuthService authService, AuthScopeFactory authScopes,
                              ExerciseAttemptService exerciseAttemptService) {
        this.frontendService = frontendService;
        this.courseService = courseService;
        this.userService = userService;
        this.exerciseService = exerciseService;
        this.authService = authService;
        this.authScopes = authScopes;
        this.exerciseAttemptService = exerciseAttemptService;
    }

    /**
     * Returns exercise by id
     * @param id Exercise id
     * @return Exercise
     * @throws Exception Something got wrong
     */
    @RequestMapping(value = {"shortInfo"}, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseInfoDto getExerciseShortInfo(@RequestParam long id,
                                                @RequestParam(value = "courseId", required = false) Long courseId,
                                                HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorized(userId, SystemPermission.SOLVE_EXERCISE, authScopes.courseOrGlobal(courseId));
        var exercise = exerciseService.getExerciseInContext(id, courseId);
        return new ExerciseInfoDto(id, exercise.getOptions());
    }

    @RequestMapping(value = {"getExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    public @NotNull ExerciseAttemptDto getExerciseAttempt(@RequestParam Long attemptId, HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUser().getId();
        exerciseAttemptService.ensureCanAccessAttempt(userId, attemptId);
        var result = frontendService.getExerciseAttempt(attemptId);
        if (result == null) {
            throw new Exception("No such attempt");
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
    public ExerciseAttemptDto getExistingExerciseAttempt(@RequestParam Long exerciseId,
                                                         @RequestParam(value = "courseId", required = false) Long courseId,
                                                         HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUser().getId();
        ensureCanSolve(userId, exerciseId, courseId);
        return frontendService.getExistingExerciseAttempt(exerciseId, userId, courseId);
    }

    @RequestMapping(value = {"createExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseAttemptDto createExerciseAttempt(@RequestParam Long exerciseId,
                                                    @RequestParam(value = "courseId", required = false) Long courseId,
                                                    HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUser().getId();
        ensureCanSolve(userId, exerciseId, courseId);
        return frontendService.createExerciseAttempt(exerciseId, userId, courseId);
    }

    @RequestMapping(value = {"createDebugExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseAttemptDto createDebugExerciseAttempt(@RequestParam Long exerciseId,
                                                         @RequestParam(value = "courseId", required = false) Long courseId,
                                                         HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorized(userId, SystemPermission.EDIT_EXERCISE, authScopes.courseOrGlobal(courseId));
        exerciseService.getExerciseInContext(exerciseId, courseId);
        return frontendService.createSolvedExerciseAttempt(exerciseId, userId, courseId);
    }

    private void ensureCanSolve(long userId, Long exerciseId, Long courseId) {
        authService.ensureAuthorized(userId, SystemPermission.SOLVE_EXERCISE, authScopes.courseOrGlobal(courseId));
        exerciseService.getExerciseInContext(exerciseId, courseId);
    }
}
