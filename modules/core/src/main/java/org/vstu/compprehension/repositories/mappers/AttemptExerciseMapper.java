package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.exerciseattempt.AttemptExerciseData;
import org.vstu.compprehension.entities.ExerciseEntity;
import org.vstu.compprehension.mappers.Mapper;

import java.util.List;

@Component
class AttemptExerciseMapper implements Mapper<ExerciseEntity, AttemptExerciseData> {

    @Override
    public @NotNull AttemptExerciseData map(@NotNull ExerciseEntity source) {
        return new AttemptExerciseData(
                source.getId(),
                source.getDomain().getName(),
                source.getStages() == null ? List.of() : List.copyOf(source.getStages()),
                source.getTags());
    }
}
