package org.vstu.compprehension.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.models.entities.EnumData.Role;

@Controller
@RequestMapping("api")
public class ExerciseSettingsController {
    private final ExerciseService exerciseService;
    private final UserService userService;

    @Autowired
    public ExerciseSettingsController(ExerciseService exerciseService, UserService userService) {
        this.exerciseService = exerciseService;
        this.userService = userService;
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise" }, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseCardDto get(@RequestParam("id") long id) {
        var currentUser = userService.getCurrentUser();
        if (!currentUser.getRoles().contains(Role.TEACHER)) {
            throw new AuthorizationServiceException("Unathorized");
        }
        return exerciseService.getExerciseCard(id);
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise"}, method = { RequestMethod.POST })
    @ResponseBody
    public void update(@RequestBody ExerciseCardDto card) {
        var currentUser = userService.getCurrentUser();
        if (!currentUser.getRoles().contains(Role.TEACHER)) {
            throw new AuthorizationServiceException("Unathorized");
        }

        exerciseService.saveExerciseCard(card);
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise"}, method = { RequestMethod.PUT })
    @ResponseBody
    public long create(@RequestBody ObjectNode json) {
        var currentUser = userService.getCurrentUser();
        if (!currentUser.getRoles().contains(Role.TEACHER)) {
            throw new AuthorizationServiceException("Unathorized");
        }

        var name = json.get("name").asText();
        var domainId = json.get("domainId").asText();
        var strategyId = json.get("strategyId").asText();

        var courseIdJson = json.get("courseId");
        var courseId = courseIdJson == null ? 1L : courseIdJson.asLong(1L);

        return exerciseService.createExercise(name, domainId, strategyId, courseId).getId();
    }
}
