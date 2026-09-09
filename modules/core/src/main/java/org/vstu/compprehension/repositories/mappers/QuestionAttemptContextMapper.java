package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.QuestionAttemptContextData;
import org.vstu.compprehension.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.mappers.Mapper;

import java.util.List;

@Component
class QuestionAttemptContextMapper implements Mapper<ExerciseAttemptEntity, QuestionAttemptContextData> {

    @Override
    public @NotNull QuestionAttemptContextData map(@NotNull ExerciseAttemptEntity source) {
        var exercise = source.getExercise();

        var context = new QuestionAttemptContextData();
        context.setAttemptId(source.getId());
        context.setUserLanguage(source.getUser().getPreferred_language());
        context.setStrategyId(exercise.getStrategyId());
        context.setStages(exercise.getStages() == null ? List.of() : List.copyOf(exercise.getStages()));
        return context;
    }
}
