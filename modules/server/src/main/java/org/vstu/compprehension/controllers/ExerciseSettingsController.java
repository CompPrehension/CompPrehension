package org.vstu.compprehension.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.models.repository.ExerciseRepository;

@Controller
@RequestMapping("api")
public class ExerciseSettingsController {
    private final ExerciseService exerciseService;

    @Autowired
    public ExerciseSettingsController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @RequestMapping(value = { "exercise" }, method = { RequestMethod.GET })
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_Administrator')")
    public ExerciseCardDto get(@RequestParam("id") long id) {
        return exerciseService.getExerciseCard(id);
    }

    @RequestMapping(value = { "exercise"}, method = { RequestMethod.POST })
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_Administrator')")
    public void update(@RequestBody ExerciseCardDto card) {
        exerciseService.saveExerciseCard(card);
    }

    @RequestMapping(value = { "exercise"}, method = { RequestMethod.PUT })
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_Administrator')")
    public long create(@RequestBody ObjectNode json) {
        var name = json.get("name").asText();
        var domainId = json.get("domainId").asText();
        var strategyId = json.get("strategyId").asText();
        return exerciseService.createExercise(name, domainId, strategyId).getId();
    }
}
