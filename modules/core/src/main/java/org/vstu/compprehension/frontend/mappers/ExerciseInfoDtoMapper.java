package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.frontend.dto.ExerciseInfoDto;
import org.vstu.compprehension.mappers.Mapping;

public interface ExerciseInfoDtoMapper extends Mapping {

    @NotNull ExerciseInfoDto map(long exerciseId, @NotNull ExerciseOptionsData options);
}
