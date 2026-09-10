package org.vstu.compprehension.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.frontend.AuthFrontendService;
import org.vstu.compprehension.frontend.ExerciseAttemptFrontendService;
import org.vstu.compprehension.frontend.ExerciseFrontendService;
import org.vstu.compprehension.frontend.UserFrontendService;
import org.vstu.compprehension.frontend.dto.ExerciseAttemptDto;
import org.vstu.compprehension.frontend.dto.ExerciseInfoDto;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;

@Controller
@RequestMapping("api/exercise")
@Log4j2
@RequiredArgsConstructor
public class ExerciseController {
    private final ExerciseAttemptFrontendService exerciseAttemptService;
    private final UserFrontendService userService;
    private final ExerciseFrontendService exerciseService;
    private final AuthFrontendService authService;

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
        var userId = userService.getCurrentUserId();
        authService.ensureAuthorized(userId, SystemPermission.SOLVE_EXERCISE, authService.courseOrGlobal(courseId));
        return exerciseService.getExerciseShortInfo(id, courseId);
    }

    @RequestMapping(value = {"getExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    public @NotNull ExerciseAttemptDto getExerciseAttempt(@RequestParam Long attemptId, HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUserId();
        exerciseAttemptService.ensureCanAccessAttempt(userId, attemptId);
        var result = exerciseAttemptService.getExerciseAttempt(attemptId);
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
        var userId = userService.getCurrentUserId();
        ensureCanSolve(userId, exerciseId, courseId);
        return exerciseAttemptService.getExistingExerciseAttempt(exerciseId, userId, courseId);
    }

    @RequestMapping(value = {"createExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseAttemptDto createExerciseAttempt(@RequestParam Long exerciseId,
                                                    @RequestParam(value = "courseId", required = false) Long courseId,
                                                    HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUserId();
        ensureCanSolve(userId, exerciseId, courseId);
        return exerciseAttemptService.createExerciseAttempt(exerciseId, userId, courseId);
    }

    @RequestMapping(value = {"createDebugExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseAttemptDto createDebugExerciseAttempt(@RequestParam Long exerciseId,
                                                         @RequestParam(value = "courseId", required = false) Long courseId,
                                                         HttpServletRequest request) throws Exception {
        var userId = userService.getCurrentUserId();
        authService.ensureAuthorized(userId, SystemPermission.EDIT_EXERCISE, authService.courseOrGlobal(courseId));
        exerciseService.ensureExerciseExists(exerciseId, courseId);
        return exerciseAttemptService.createSolvedExerciseAttempt(exerciseId, userId, courseId);
    }

    private void ensureCanSolve(long userId, Long exerciseId, Long courseId) {
        authService.ensureAuthorized(userId, SystemPermission.SOLVE_EXERCISE, authService.courseOrGlobal(courseId));
        exerciseService.ensureExerciseExists(exerciseId, courseId);
    }
}
