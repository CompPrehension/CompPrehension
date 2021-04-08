package org.vstu.compprehension.controllers.interfaces;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.dto.ExerciseStatisticsItemDto;

public interface ExerciseStatisticsController {
    @RequestMapping(value = {"/getExerciseStatistics"}, method = { RequestMethod.GET })
    @ResponseBody
    ExerciseStatisticsItemDto[] getExerciseStatistics(Long exerciseId);
}
