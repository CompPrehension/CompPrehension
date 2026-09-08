package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.QuestionAttemptContextData;
import org.vstu.compprehension.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.mappers.Mapper;

import java.util.List;

/** Условия, в которых задан вопрос: упражнение и пользователь должны быть подгружены. */
@Component
class QuestionAttemptContextMapper implements Mapper<ExerciseAttemptEntity, QuestionAttemptContextData> {

    @Override
    public @NotNull QuestionAttemptContextData map(@NotNull ExerciseAttemptEntity source) {
        var exercise = source.getExercise();
        return new QuestionAttemptContextData(
                source.getId(),
                source.getUser().getPreferred_language(),
                exercise.getStrategyId(),
                exercise.getStages() == null ? List.of() : List.copyOf(exercise.getStages()),
                exercise.getOptions().isPreferDecisionTreeBasedSupplementaryEnabled());
    }
}
