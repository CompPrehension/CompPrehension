package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.Nullable;

public record AttemptOwnerData(long userId, @Nullable Long courseId) {
}
