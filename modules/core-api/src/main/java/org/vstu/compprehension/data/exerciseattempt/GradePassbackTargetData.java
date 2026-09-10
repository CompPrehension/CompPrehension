package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.EducationResourceType;

/**
 * Куда и за кого отправлять оценку за попытку.
 */
public record GradePassbackTargetData(
        long attemptId,
        long exerciseId,
        long userId,
        @Nullable String externalUserId,
        @Nullable String ltiLineitemUrl,
        @Nullable CourseTarget course) {

    public record CourseTarget(
            long courseId,
            @Nullable String externalCourseId,
            @NotNull EducationResourceTarget educationResource) {
    }

    public record EducationResourceTarget(
            long id,
            @NotNull EducationResourceType type,
            @NotNull String url) {
    }
}
