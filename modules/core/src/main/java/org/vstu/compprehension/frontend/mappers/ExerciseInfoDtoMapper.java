package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.frontend.dto.ExerciseInfoDto;
import org.vstu.compprehension.mappers.UpdateMapper;

@Component
class ExerciseInfoDtoMapper implements UpdateMapper<ExerciseOptionsData, ExerciseInfoDto> {
    @Override
    public void apply(@NotNull ExerciseOptionsData source, @NotNull ExerciseInfoDto destination) {
        destination.setOptions(source);
    }
}
