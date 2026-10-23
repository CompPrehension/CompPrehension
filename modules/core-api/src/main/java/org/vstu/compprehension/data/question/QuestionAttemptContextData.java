package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exercise.ExerciseStageData;
import org.vstu.compprehension.enums.Language;

public record QuestionAttemptContextData(
        long attemptId,
        Language userLanguage,
        String strategyId,
        @NotNull ExerciseStageData questionStage) {

    public QuestionAttemptContextData(@NotNull ExerciseAttemptContextData attempt,
                                      @NotNull ExerciseStageData questionStage) {
        this(attempt.attemptId(), attempt.userLanguage(), attempt.strategyId(), questionStage);
    }
}
