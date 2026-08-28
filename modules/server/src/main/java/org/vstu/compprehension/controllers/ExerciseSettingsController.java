package org.vstu.compprehension.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.AuthService;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.ExercisePermissionService;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.dto.ExerciseListDto;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.Permission;

import java.util.List;

@Controller
@RequestMapping("api")
public class ExerciseSettingsController {
    private final ExerciseService exerciseService;
    private final CourseService courseService;
    private final UserService userService;
    private final AuthService authService;
    private final ExercisePermissionService exercisePermissionService;

    @Autowired
    public ExerciseSettingsController(ExerciseService exerciseService,
                                      CourseService courseService,
                                      UserService userService,
                                      AuthService authService,
                                      ExercisePermissionService exercisePermissionService) {
        this.exerciseService = exerciseService;
        this.courseService = courseService;
        this.userService = userService;
        this.authService = authService;
        this.exercisePermissionService = exercisePermissionService;
    }

    @SneakyThrows
    @RequestMapping(value = {"exercise"}, method = {RequestMethod.GET})
    @ResponseBody
    public ExerciseCardDto get(@RequestParam("id") long id, @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorized(userId, Permission.VIEW_EXERCISE, courseId);
        if (courseId != null) {
            courseService.findExerciseCourseLinkOrThrow(id, courseId);
        }
        var exercise = exerciseService.getExercise(id);
        return exerciseService.getExerciseCard(
                exercise, exercisePermissionService.ofExercise(userId, exercise, courseId));
    }

    @SneakyThrows
    @RequestMapping(value = {"exercise/list"}, method = {RequestMethod.GET})
    @ResponseBody
    public ExerciseListDto list(@RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorized(userId, Permission.VIEW_EXERCISE, courseId);
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
        authService.ensureAuthorized(userId, Permission.EDIT_EXERCISE, courseId);
        if (courseId != null) {
            courseService.findExerciseCourseLinkOrThrow(card.getId(), courseId);
            ensureNotInherited(card.getId(), courseId);
        }
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
        authService.ensureAuthorized(userId, Permission.CREATE_EXERCISE, courseId);
        return exerciseService.createExercise(name, domainId, strategyId, courseId).getId();
    }

    @SneakyThrows
    @RequestMapping(value = {"exercise/{id}/clone"}, method = {RequestMethod.POST})
    @ResponseBody
    public long clone(@PathVariable("id") long id,
                      @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorized(userId, Permission.CREATE_EXERCISE, courseId);
        return exerciseService.cloneExercise(id, courseId).getId();
    }

    @SneakyThrows
    @ResponseBody
    @RequestMapping(value = {"exercise"}, method = {RequestMethod.DELETE})
    public void delete(@RequestParam("id") long id, @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorized(userId, Permission.DELETE_EXERCISE, courseId);
        if (courseId != null) {
            courseService.findExerciseCourseLinkOrThrow(id, courseId);
            ensureNotInherited(id, courseId);
        }
        exerciseService.deleteExercise(id);
    }

    /**
     * Предусловие состояния, а не прав: наследованное из глобального пула упражнение из курса
     * только читается. Тот же признак гасит кнопки в карточке — см.
     * {@link org.vstu.compprehension.Service.ExercisePermissionService#ofExercise}.
     */
    private void ensureNotInherited(long exerciseId, long courseId) {
        if (ExerciseService.isInheritedInCourse(exerciseService.getExercise(exerciseId), courseId)) {
            throw new IllegalStateException("inherited_exercise_is_read_only");
        }
    }
}
