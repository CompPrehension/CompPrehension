package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.ExerciseAttemptContextData;
import org.vstu.compprehension.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.mappers.Mapper;

import java.util.List;

@Component
class QuestionAttemptContextMapper implements Mapper<ExerciseAttemptEntity, ExerciseAttemptContextData> {

    @Override
    public @NotNull ExerciseAttemptContextData map(@NotNull ExerciseAttemptEntity source) {
        var exercise = source.getExercise();
        return new ExerciseAttemptContextData(
                source.getId(),
                source.getUser().getPreferred_language(),
                exercise.getStrategyId(),
                exercise.getStages() == null ? List.of() : List.copyOf(exercise.getStages()));
    }
}
