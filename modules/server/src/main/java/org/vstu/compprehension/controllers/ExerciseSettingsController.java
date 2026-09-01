package org.vstu.compprehension.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.AuthService;
import org.vstu.compprehension.Service.AuthScopeFactory;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.ExercisePermissionService;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.dto.ExerciseListDto;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;

import java.util.List;

@Controller
@RequestMapping("api")
public class ExerciseSettingsController {
    private final ExerciseService exerciseService;
    private final CourseService courseService;
    private final UserService userService;
    private final AuthService authService;
    private final AuthScopeFactory authScopes;
    private final ExercisePermissionService exercisePermissionService;

    @Autowired
    public ExerciseSettingsController(ExerciseService exerciseService,
                                      CourseService courseService,
                                      UserService userService,
                                      AuthService authService,
                                      AuthScopeFactory authScopes,
                                      ExercisePermissionService exercisePermissionService) {
        this.exerciseService = exerciseService;
        this.courseService = courseService;
        this.userService = userService;
        this.authService = authService;
        this.authScopes = authScopes;
        this.exercisePermissionService = exercisePermissionService;
    }

    @SneakyThrows
    @RequestMapping(value = {"exercise"}, method = {RequestMethod.GET})
    @ResponseBody
    public ExerciseCardDto get(@RequestParam("id") long id, @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorized(userId, SystemPermission.VIEW_EXERCISE, authScopes.courseOrGlobal(courseId));
        var exercise = exerciseService.getExerciseInContext(id, courseId);
        return exerciseService.getExerciseCard(
                exercise, exercisePermissionService.ofExercise(userId, exercise, courseId));
    }

    @SneakyThrows
    @RequestMapping(value = {"exercise/list"}, method = {RequestMethod.GET})
    @ResponseBody
    public ExerciseListDto list(@RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorized(userId, SystemPermission.VIEW_EXERCISE, authScopes.courseOrGlobal(courseId));
        List<ExerciseDto> exercises = courseId != null
                ? exerciseService.getCourseExercises(courseId)
                : exerciseService.getPublicExercises();
        return new ExerciseListDto(exercises, exercisePermissionService.ofExerciseList(userId, courseId));
    }

    @SneakyThrows
    @RequestMapping(value = {"exercise"}, method = {RequestMethod.POST})
    @ResponseBody
    public void update(@RequestBody ExerciseCardDto card, @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorized(userId, SystemPermission.EDIT_EXERCISE, authScopes.courseOrGlobal(courseId));
        ensureNotInherited(exerciseService.getExerciseInContext(card.getId(), courseId), courseId);
        exerciseService.saveExerciseCard(card);
    }

    @SneakyThrows
    @RequestMapping(value = {"exercise"}, method = {RequestMethod.PUT})
    @ResponseBody
    public long create(@RequestBody ObjectNode json) {
        var userId = userService.getCurrentUser().getId();
        var name = json.get("name").asText();
        var domainId = json.get("domainId").asText();
        var strategyId = json.get("strategyId").asText();
        var courseId = json.has("courseId") && !json.get("courseId").isNull()
                ? json.get("courseId").asLong()
                : null;
        authService.ensureAuthorized(userId, SystemPermission.CREATE_EXERCISE, authScopes.courseOrGlobal(courseId));
        return exerciseService.createExercise(name, domainId, strategyId, courseId).getId();
    }

    @SneakyThrows
    @RequestMapping(value = {"exercise/{id}/clone"}, method = {RequestMethod.POST})
    @ResponseBody
    public long clone(@PathVariable("id") long id,
                      @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUser().getId();
        // courseId здесь — куда клонируем, поэтому доступ к источнику проверяется отдельно,
        // в его собственном контексте.
        authService.ensureAuthorized(userId, SystemPermission.CREATE_EXERCISE, authScopes.courseOrGlobal(courseId));
        ensureCanViewSource(userId, exerciseService.getExercise(id));
        return exerciseService.cloneExercise(id, courseId).getId();
    }

    @SneakyThrows
    @ResponseBody
    @RequestMapping(value = {"exercise"}, method = {RequestMethod.DELETE})
    public void delete(@RequestParam("id") long id, @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorized(userId, SystemPermission.DELETE_EXERCISE, authScopes.courseOrGlobal(courseId));
        ensureNotInherited(exerciseService.getExerciseInContext(id, courseId), courseId);
        exerciseService.deleteExercise(id);
    }

    /**
     * Может ли пользователь читать упражнение в его собственном контексте: публичное — по правам
     * в GLOBAL-области, приватное — по правам хотя бы в одном из курсов, к которым оно привязано.
     */
    private void ensureCanViewSource(long userId, ExerciseEntity exercise) {
        if (exercise.isPublic() && authService.isAuthorized(userId, SystemPermission.VIEW_EXERCISE, authScopes.global())) {
            return;
        }
        var courseIds = courseService.findCourseIdsByExerciseId(exercise.getId());
        if (!authService.isAuthorized(userId, SystemPermission.VIEW_EXERCISE, authScopes.anyOfCourses(courseIds))) {
            throw new SecurityException(String.format(
                    "User %s is not allowed to read exercise %s", userId, exercise.getId()));
        }
    }

    private static void ensureNotInherited(ExerciseEntity exercise, Long courseId) {
        if (ExerciseService.isInheritedInCourse(exercise, courseId)) {
            throw new IllegalStateException("inherited_exercise_is_read_only");
        }
    }
}
