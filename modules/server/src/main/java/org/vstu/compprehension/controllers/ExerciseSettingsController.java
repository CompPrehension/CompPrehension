package org.vstu.compprehension.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.models.entities.EnumData.Role;

@Controller
@RequestMapping("api")
public class ExerciseSettingsController {
    private final ExerciseService exerciseService;
    private final CourseService courseService;
    private final UserService userService;

    @Autowired
    public ExerciseSettingsController(ExerciseService exerciseService, CourseService courseService, UserService userService) {
        this.exerciseService = exerciseService;
        this.courseService = courseService;
        this.userService = userService;
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise" }, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseCardDto get(@RequestParam("id") long id, @RequestParam(value = "courseId", required = false) Long courseId) {
        var currentUser = userService.getCurrentUser();
        if (!currentUser.getRoles().contains(Role.TEACHER)) {
            throw new AuthorizationServiceException("Unathorized");
        }
        if (courseId != null) {
            courseService.findExerciseCourseLinkOrThrow(id, courseId);
        }
        return exerciseService.getExerciseCard(id);
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise"}, method = { RequestMethod.POST })
    @ResponseBody
    public void update(@RequestBody ExerciseCardDto card, @RequestParam(value = "courseId", required = false) Long courseId) {
        var currentUser = userService.getCurrentUser();
        if (!currentUser.getRoles().contains(Role.TEACHER)) {
            throw new AuthorizationServiceException("Unathorized");
        }
        if (courseId != null) {
            courseService.findExerciseCourseLinkOrThrow(card.getId(), courseId);
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
        return exerciseService.createExercise(name, domainId, strategyId).getId();
    }
}
