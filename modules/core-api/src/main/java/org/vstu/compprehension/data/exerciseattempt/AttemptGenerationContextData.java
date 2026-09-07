package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.data.question.QuestionAttemptContextData;

public record AttemptGenerationContextData(
        long attemptId,
        @NotNull String domainId,
        @NotNull String strategyId,
        @NotNull ExerciseOptionsData exerciseOptions,
        @NotNull Language userLanguage) {
}
