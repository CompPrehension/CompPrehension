package org.vstu.compprehension.data.exercise;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ExerciseCardUpdateData(
        long id,
        @NotNull String name,
        @NotNull String domainId,
        @NotNull String backendId,
        @NotNull String strategyId,
        @NotNull ExerciseOptionsData options,
        @NotNull List<ExerciseStageData> stages,
        @NotNull List<String> tags) {
}
