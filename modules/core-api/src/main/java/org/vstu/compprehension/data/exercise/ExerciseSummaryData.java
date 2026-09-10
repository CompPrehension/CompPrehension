package org.vstu.compprehension.data.exercise;

import org.jetbrains.annotations.NotNull;

public record ExerciseSummaryData(long id, @NotNull String name, boolean isPublic) {
}
