package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exercise.ExerciseStageData;
import org.vstu.compprehension.enums.Language;

import java.util.List;

public record QuestionAttemptContextData(
        long attemptId,
        @NotNull Language userLanguage,
        @NotNull String strategyId,
        @NotNull List<ExerciseStageData> stages,
        boolean preferDecisionTreeSupplementary) {
}
