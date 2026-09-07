package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exercise.ExerciseStageData;

import java.util.List;

public record AttemptExerciseData(
        long id,
        @NotNull String domainName,
        @NotNull List<ExerciseStageData> stages,
        @NotNull List<String> tags) {
}
