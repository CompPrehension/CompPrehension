package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.cource.CourseExerciseData;
import org.vstu.compprehension.frontend.dto.ExerciseRefDto;
import org.vstu.compprehension.mappers.Mapper;

@Component
class ExerciseRefDtoMapper implements Mapper<CourseExerciseData, ExerciseRefDto> {

    @Override
    public @NotNull ExerciseRefDto map(@NotNull CourseExerciseData source) {
        return new ExerciseRefDto(source.exerciseId(), source.name());
    }
}
