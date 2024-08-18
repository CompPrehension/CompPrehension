package org.vstu.compprehension.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.AuthorizationService;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects;
import org.vstu.compprehension.models.entities.EnumData.Role;

@Controller
@RequestMapping("api")
public class ExerciseSettingsController {
    private final ExerciseService exerciseService;
    private final UserService userService;
    private final AuthorizationService authorizationService;

    @Autowired
    public ExerciseSettingsController(ExerciseService exerciseService, UserService userService, AuthorizationService authorizationService) {
        this.exerciseService = exerciseService;
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise" }, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseCardDto get(@RequestParam("id") long id) {
        var currentUser = userService.getCurrentUser();
        var courceId = 1;
        if (!authorizationService.isAuthorizedCourse(currentUser.getId(), AuthObjects.Permissions.ViewExercise.Name(), courceId)) {
            throw new AuthorizationServiceException("Authorization error");
        }

        return exerciseService.getExerciseCard(id);
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise"}, method = { RequestMethod.POST })
    @ResponseBody
    public void update(@RequestBody ExerciseCardDto card) {
        var currentUser = userService.getCurrentUser();
        var courceId = 1;
        if (!authorizationService.isAuthorizedCourse(currentUser.getId(), AuthObjects.Permissions.EditExercise.Name(), courceId)) {
            throw new AuthorizationServiceException("Authorization error");
        }

        exerciseService.saveExerciseCard(card);
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise"}, method = { RequestMethod.PUT })
    @ResponseBody
    public long create(@RequestBody ObjectNode json) {
        var currentUser = userService.getCurrentUser();
        var courceId = 1;
        if (!authorizationService.isAuthorizedCourse(currentUser.getId(), AuthObjects.Permissions.CreateExercise.Name(), courceId)) {
            throw new AuthorizationServiceException("Authorization error");
        }

        var name = json.get("name").asText();
        var domainId = json.get("domainId").asText();
        var strategyId = json.get("strategyId").asText();
        return exerciseService.createExercise(name, domainId, strategyId).getId();
    }
}
