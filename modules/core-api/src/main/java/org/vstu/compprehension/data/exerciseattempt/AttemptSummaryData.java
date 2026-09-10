package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.AttemptStatus;

import java.util.List;

public record AttemptSummaryData(
        long attemptId,
        long userId,
        long exerciseId,
        @Nullable Long courseId,
        @NotNull AttemptStatus status,
        @NotNull List<Long> questionIds) {
}
