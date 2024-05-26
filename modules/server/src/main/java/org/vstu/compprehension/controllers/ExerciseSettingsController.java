package org.vstu.compprehension.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.AuthorizationService;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects;

import java.util.Optional;

@Controller
@RequestMapping("api")
public class ExerciseSettingsController {
    private final ExerciseService exerciseService;
    private final UserService userService;
    private final CourseService courseService;
    private final AuthorizationService authorizationService;

    @Autowired
    public ExerciseSettingsController(ExerciseService exerciseService, UserService userService, CourseService courseService, AuthorizationService authorizationService) {
        this.exerciseService = exerciseService;
        this.userService = userService;
        this.courseService = courseService;
        this.authorizationService = authorizationService;
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise" }, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseCardDto get(@RequestParam("id") long id) {
        var exerciseCard = exerciseService.getExerciseCard(id);
        if (!isAuthorizedByExerciseCard(exerciseCard)) {
            throw new AuthorizationServiceException("Unathorized");
        }

        return exerciseCard;
    }

    private boolean isAuthorizedByExerciseCard(ExerciseCardDto exerciseCard) throws Exception {
        var courseId = exerciseCard.getCourseId();
        var currentUser = userService.getCurrentUser();

        return authorizationService.isAuthorizedAnyCourseOrGlobal(
                currentUser.getId(),
                AuthObjects.Permissions.EditExercise.Name(),
                Optional.of(courseId));
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise"}, method = { RequestMethod.POST })
    @ResponseBody
    public void update(@RequestBody ExerciseCardDto updatedCard) {
        var exerciseCard = exerciseService.getExerciseCard(updatedCard.getId());
        if (!isAuthorizedByExerciseCard(exerciseCard)) {
            throw new AuthorizationServiceException("Unathorized");
        }

        exerciseService.saveExerciseCard(updatedCard);
    }

    @SneakyThrows
    @RequestMapping(value = { "exercise"}, method = { RequestMethod.PUT })
    @ResponseBody
    public long create(@RequestBody ObjectNode json) {
        var courseIdJson = json.get("courseId");
        var initCourse = courseService.getInitialCourseId();
        var courseId = courseIdJson == null ? initCourse : courseIdJson.asLong(initCourse);

        var currentUser = userService.getCurrentUser();

        var isAuthorized = authorizationService.isAuthorizedAnyCourseOrGlobal(
                currentUser.getId(),
                AuthObjects.Permissions.CreateExercise.Name(),
                Optional.of(courseId));

        if (!isAuthorized) {
            throw new AuthorizationServiceException("Unathorized");
        }

        var name = json.get("name").asText();
        var domainId = json.get("domainId").asText();
        var strategyId = json.get("strategyId").asText();

        return exerciseService.createExercise(name, domainId, strategyId, courseId).getId();
    }
}
