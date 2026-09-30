package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.Nullable;

/**
 * Куда и за кого отправлять оценку за попытку.
 */
public record GradePassbackTargetData(
        long attemptId,
        long exerciseId,
        long userId,
        @Nullable String externalUserId,
        @Nullable String ltiLineitemUrl,
        @Nullable String ltiIssuer,
        @Nullable String ltiClientId,
        @Nullable Long courseId) {
}
