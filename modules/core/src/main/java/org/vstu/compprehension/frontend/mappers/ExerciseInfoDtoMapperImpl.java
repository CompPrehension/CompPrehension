package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.frontend.dto.ExerciseInfoDto;

@Component
class ExerciseInfoDtoMapperImpl implements ExerciseInfoDtoMapper {

    @Override
    public @NotNull ExerciseInfoDto map(long exerciseId, @NotNull ExerciseOptionsData options) {
        return new ExerciseInfoDto(exerciseId, options);
    }
}
