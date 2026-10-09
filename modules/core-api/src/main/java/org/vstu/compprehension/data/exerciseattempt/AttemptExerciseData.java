package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.exercise.ExerciseStageData;

import java.util.List;
import java.util.Map;

public record AttemptExerciseData(
        long id,
        @NotNull String domainId,
        @NotNull List<ExerciseStageData> stages,
        @NotNull List<String> tags,
        @Nullable Map<String, Object> strategySettings) {
}
