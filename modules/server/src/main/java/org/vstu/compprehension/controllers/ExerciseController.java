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
import org.vstu.compprehension.Service.AuthService;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.ExerciseAttemptService;
import org.vstu.compprehension.Service.FrontendService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.ExerciseAttemptDto;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.dto.ExerciseInfoDto;
import org.vstu.compprehension.dto.ExerciseStatisticsItemDto;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.Permission;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.repository.ExerciseRepository;

import java.util.List;

@Controller
@RequestMapping("api/exercise")
@Log4j2
public class ExerciseController {
    private final FrontendService frontendService;
    private final CourseService courseService;
    private final UserService userService;
    private final ExerciseRepository exerciseRepository;
    private final AuthService authService;
    private final ExerciseAttemptService exerciseAttemptService;

    @Autowired
    public ExerciseController(FrontendService frontendService, CourseService courseService, UserService userService,
                              ExerciseRepository exerciseRepository, AuthService authService,
                              ExerciseAttemptService exerciseAttemptService) {
        this.frontendService = frontendService;
        this.courseService = courseService;
        this.userService = userService;
        this.exerciseRepository = exerciseRepository;
        this.authService = authService;
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
        authService.ensureAuthorized(userId, Permission.SOLVE_EXERCISE, courseId);
        ExerciseEntity exercise;
        if (courseId != null) {
            exercise = courseService.findExerciseCourseLinkOrThrow(id, courseId).getExercise();
        } else {
            exercise = exerciseRepository.findById(id).orElseThrow();
        }
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
        authService.ensureAuthorized(userId, Permission.EDIT_EXERCISE, courseId);
        if (courseId != null) {
            courseService.findExerciseCourseLinkOrThrow(exerciseId, courseId);
        }
        return frontendService.createSolvedExerciseAttempt(exerciseId, userId, courseId);
    }

    private void ensureCanSolve(long userId, Long exerciseId, Long courseId) {
        authService.ensureAuthorized(userId, Permission.SOLVE_EXERCISE, courseId);
        if (courseId != null) {
            courseService.findExerciseCourseLinkOrThrow(exerciseId, courseId);
        }
    }
}
