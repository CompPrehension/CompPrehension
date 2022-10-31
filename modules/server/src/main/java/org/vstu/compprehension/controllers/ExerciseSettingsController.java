package org.vstu.compprehension.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.Service.ExerciseService;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.models.repository.ExerciseRepository;

import java.util.List;

@Controller
@RequestMapping({"basic/exercise", "lti/exercise" })
public class ExerciseSettingsController {
    private final ExerciseRepository exerciseRepository;
    private final ExerciseService exerciseService;

    @Autowired
    public ExerciseSettingsController(ExerciseRepository exerciseRepository, ExerciseService exerciseService) {
        this.exerciseRepository = exerciseRepository;
        this.exerciseService = exerciseService;
    }

    @RequestMapping(value = {"/all"}, method = { RequestMethod.GET })
    @ResponseBody
    List<ExerciseDto> getAll() {
        return exerciseRepository.getAllExerciseItems();
    }

    @RequestMapping(value = {""}, method = { RequestMethod.GET })
    @ResponseBody
    ExerciseCardDto get(@RequestParam("id") long id) {
        return exerciseService.getExerciseCard(id);
    }
}
