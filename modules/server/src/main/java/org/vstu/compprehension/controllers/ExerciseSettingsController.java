package org.vstu.compprehension.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.models.businesslogic.user.UserContext;
import org.vstu.compprehension.models.entities.EnumData.Role;
import org.vstu.compprehension.models.repository.ExerciseRepository;

import java.util.List;

@Controller
@RequestMapping({"basic/exercise", "lti/exercise" })
public class ExerciseSettingsController {
    private final ExerciseRepository exerciseRepository;
    private final ExerciseService exerciseService;
    private final UserContext currentUser;

    @Autowired
    public ExerciseSettingsController(ExerciseRepository exerciseRepository, ExerciseService exerciseService, UserContext currentUser) {
        this.exerciseRepository = exerciseRepository;
        this.exerciseService = exerciseService;
        this.currentUser = currentUser;
    }

    @RequestMapping(value = {"/all"}, method = { RequestMethod.GET })
    @ResponseBody
    public List<ExerciseDto> getAll() {
        return exerciseRepository.getAllExerciseItems();
    }


    @RequestMapping(value = {""}, method = { RequestMethod.GET })
    @ResponseBody
    public ExerciseCardDto get(@RequestParam("id") long id) {
        if (!currentUser.getRoles().contains(Role.TEACHER))
            throw new AuthorizationServiceException("Current user dont have TEACHER rights");
        return exerciseService.getExerciseCard(id);
    }

    @RequestMapping(value = {""}, method = { RequestMethod.POST })
    @ResponseBody
    public void update(@RequestBody ExerciseCardDto card) {
        if (!currentUser.getRoles().contains(Role.TEACHER))
            throw new AuthorizationServiceException("Current user dont have TEACHER rights");

        exerciseService.saveExerciseCard(card);
    }

    @RequestMapping(value = {""}, method = { RequestMethod.PUT })
    @ResponseBody
    public long create(@RequestBody ObjectNode json) {
        if (!currentUser.getRoles().contains(Role.TEACHER))
            throw new AuthorizationServiceException("Current user dont have TEACHER rights");

        var name = json.get("name").asText();
        var domainId = json.get("domainId").asText();
        var strategyId = json.get("strategyId").asText();
        return exerciseService.createExercise(name, domainId, strategyId).getId();
    }
}
