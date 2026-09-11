package org.vstu.compprehension.controllers;

import tools.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.frontend.AuthFrontendService;
import org.vstu.compprehension.frontend.ExerciseAttemptFrontendService;
import org.vstu.compprehension.frontend.ExerciseFrontendService;
import org.vstu.compprehension.frontend.UserFrontendService;
import org.vstu.compprehension.frontend.dto.ExerciseCardDto;
import org.vstu.compprehension.frontend.dto.ExerciseListDto;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;

@Controller
@RequestMapping("api")
@RequiredArgsConstructor
public class ExerciseSettingsController {
    private final ExerciseFrontendService exerciseService;
    private final UserFrontendService userService;
    private final AuthFrontendService authService;

    @SneakyThrows
    @RequestMapping(value = {"exercise"}, method = {RequestMethod.GET})
    @ResponseBody
    public ExerciseCardDto get(@RequestParam("id") long id, @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUserId();
        authService.ensureAuthorized(userId, SystemPermission.VIEW_EXERCISE, authService.courseOrGlobal(courseId));
        return exerciseService.getExerciseCard(id, courseId, userId);
    }

    @SneakyThrows
    @RequestMapping(value = {"exercise/list"}, method = {RequestMethod.GET})
    @ResponseBody
    public ExerciseListDto list(@RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUserId();
        authService.ensureAuthorized(userId, SystemPermission.VIEW_EXERCISE, authService.courseOrGlobal(courseId));
        return exerciseService.listExercises(courseId, userId);
    }

    @SneakyThrows
    @RequestMapping(value = {"exercise"}, method = {RequestMethod.POST})
    @ResponseBody
    public void update(@RequestBody ExerciseCardDto card, @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUserId();
        authService.ensureAuthorized(userId, SystemPermission.EDIT_EXERCISE, authService.courseOrGlobal(courseId));
        exerciseService.saveExerciseCard(card, courseId);
    }

    @SneakyThrows
    @RequestMapping(value = {"exercise"}, method = {RequestMethod.PUT})
    @ResponseBody
    public long create(@RequestBody ObjectNode json) {
        var userId = userService.getCurrentUserId();
        var name = json.get("name").asText();
        var domainId = json.get("domainId").asText();
        var strategyId = json.get("strategyId").asText();
        var courseId = json.has("courseId") && !json.get("courseId").isNull()
                ? json.get("courseId").asLong()
                : null;
        authService.ensureAuthorized(userId, SystemPermission.CREATE_EXERCISE, authService.courseOrGlobal(courseId));
        return exerciseService.createExerciseAndGetId(name, domainId, strategyId, courseId);
    }

    @SneakyThrows
    @RequestMapping(value = {"exercise/{id}/clone"}, method = {RequestMethod.POST})
    @ResponseBody
    public long clone(@PathVariable("id") long id,
                      @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUserId();
        authService.ensureAuthorized(userId, SystemPermission.CREATE_EXERCISE, authService.courseOrGlobal(courseId));
        exerciseService.ensureCanViewExercise(userId, id);
        return exerciseService.cloneExerciseAndGetId(id, courseId);
    }

    @SneakyThrows
    @ResponseBody
    @RequestMapping(value = {"exercise"}, method = {RequestMethod.DELETE})
    public void delete(@RequestParam("id") long id, @RequestParam(value = "courseId", required = false) Long courseId) {
        var userId = userService.getCurrentUserId();
        authService.ensureAuthorized(userId, SystemPermission.DELETE_EXERCISE, authService.courseOrGlobal(courseId));
        exerciseService.deleteExercise(id, courseId);
    }

}
