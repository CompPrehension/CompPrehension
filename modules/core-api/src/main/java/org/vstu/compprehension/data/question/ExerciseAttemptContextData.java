package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exercise.ExerciseStageData;
import org.vstu.compprehension.enums.Language;

import java.util.List;

public record ExerciseAttemptContextData(
        long attemptId,
        Language userLanguage,
        String strategyId,
        @NotNull List<ExerciseStageData> stages) {
}
