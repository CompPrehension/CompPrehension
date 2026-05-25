package org.vstu.compprehension.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.AuthService;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.models.entities.EnumData.Permission;

import java.util.List;

@Controller
@RequestMapping("api")
public class ExerciseSettingsController {
    private final ExerciseService exerciseService;
    private final CourseService courseService;
    private final UserService userService;
    private final AuthService authService;

    @Autowired
    public ExerciseSettingsController(ExerciseService exerciseService,
                                      CourseService courseService,
                                      UserService userService,
                                      AuthService authService) {
        this.exerciseService = exerciseService;
        this.courseService = courseService;
        this.userService = userService;
        this.authService = authService;
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise" }, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseCardDto get(@RequestParam("id") long id, @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUser().getId();
        // Reading a card is a view operation: gate on VIEW_EXERCISE so students can open
        // (but not edit) exercises. Mutations below stay on EDIT/CREATE/DELETE.
        if (courseId != null) {
            authService.ensureAuthorizedInCourse(userId, Permission.VIEW_EXERCISE, courseId);
            courseService.findExerciseCourseLinkOrThrow(id, courseId);
        } else {
            authService.ensureAuthorizedGlobal(userId, Permission.VIEW_EXERCISE);
        }
        return exerciseService.getExerciseCard(id);
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise/list" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<ExerciseDto> list(@RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUser().getId();
        if (courseId != null) {
            authService.ensureAuthorizedInCourse(userId, Permission.VIEW_EXERCISE, courseId);
            return exerciseService.getCourseExercises(courseId);
        }
        authService.ensureAuthorizedGlobal(userId, Permission.VIEW_EXERCISE);
        return exerciseService.getPublicExercises();
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise/global-pool" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<ExerciseDto> getGlobalPool() {
        return exerciseService.getPublicExercises();
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise"}, method = { RequestMethod.POST })
    @ResponseBody
    public void update(@RequestBody ExerciseCardDto card, @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUser().getId();
        if (courseId != null) {
            authService.ensureAuthorizedInCourse(userId, Permission.EDIT_EXERCISE, courseId);
            courseService.findExerciseCourseLinkOrThrow(card.getId(), courseId);
        } else {
            authService.ensureAuthorizedGlobal(userId, Permission.EDIT_EXERCISE);
        }
        exerciseService.saveExerciseCard(card);
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise"}, method = { RequestMethod.PUT })
    @ResponseBody
    public long create(@RequestBody ObjectNode json) {
        var userId = userService.getCurrentUser().getId();
        var name = json.get("name").asText();
        var domainId = json.get("domainId").asText();
        var strategyId = json.get("strategyId").asText();
        var courseId = json.has("courseId") && !json.get("courseId").isNull()
            ? json.get("courseId").asLong()
            : null;
        if (courseId != null) {
            authService.ensureAuthorizedInCourse(userId, Permission.CREATE_EXERCISE, courseId);
        } else {
            authService.ensureAuthorizedGlobal(userId, Permission.CREATE_EXERCISE);
        }
        return exerciseService.createExercise(name, domainId, strategyId, courseId).getId();
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise/{id}/clone"}, method = { RequestMethod.POST })
    @ResponseBody
    public long clone(@PathVariable("id") long id,
                      @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUser().getId();
        if (courseId != null) {
            authService.ensureAuthorizedInCourse(userId, Permission.CREATE_EXERCISE, courseId);
        } else {
            authService.ensureAuthorizedGlobal(userId, Permission.CREATE_EXERCISE);
        }
        return exerciseService.cloneExercise(id, courseId).getId();
    }

    @SneakyThrows
    @ResponseBody
    @RequestMapping(value = { "exercise"}, method = { RequestMethod.DELETE })
    public void delete(@RequestParam("id") long id) {
        var userId = userService.getCurrentUser().getId();
        authService.ensureAuthorizedGlobal(userId, Permission.DELETE_EXERCISE);
        exerciseService.deleteExercise(id);
    }
}
