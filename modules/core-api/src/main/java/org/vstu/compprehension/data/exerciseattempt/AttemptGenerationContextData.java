package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;

public record AttemptGenerationContextData(
        long attemptId,
        @NotNull String domainId,
        @NotNull ExerciseOptionsData exerciseOptions,
        @NotNull Language userLanguage) {
}
