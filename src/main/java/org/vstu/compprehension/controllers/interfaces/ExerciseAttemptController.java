package org.vstu.compprehension.controllers.interfaces;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.dto.ExerciseAttemptDto;

import javax.servlet.http.HttpServletRequest;

public interface ExerciseAttemptController {
    @RequestMapping(value = {"/getExistingExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    ExerciseAttemptDto getExistingExerciseAttempt(Long exerciseId, HttpServletRequest request);

    @RequestMapping(value = {"/createExerciseAttempt"}, method = { RequestMethod.GET })
    @ResponseBody
    ExerciseAttemptDto createExerciseAttempt(Long exerciseId, HttpServletRequest request);
}
