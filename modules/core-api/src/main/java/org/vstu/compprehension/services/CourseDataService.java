package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.lti.LtiContext;
import org.vstu.compprehension.data.cource.CourseExerciseData;
import org.vstu.compprehension.frontend.dto.course.CourseDto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseDataService {
    @NotNull Optional<Long> findCourseIdByExternalIdAndResourceId(@NotNull String externalCourseId, long educationResourceId);

    @NotNull Optional<Long> resolveOrCreateIdFromLtiContext(@NotNull LtiContext ctx, long educationResourceId);

    void linkExerciseWithCourseIfMissing(long exerciseId, long courseId);

    @NotNull List<Long> findCourseIdsByExerciseId(long exerciseId);

    @NotNull List<CourseExerciseData> getExercisesInCourseOrThrow(long courseId, @NotNull Collection<Long> exerciseIds);

    void ensureExerciseInCourse(long exerciseId, long courseId);

    @NotNull List<CourseDto> getUserCourses(long userId);

    @NotNull List<CourseDto> getExerciseMemberships(long exerciseId);

    void addExerciseToCourse(long exerciseId, long courseId);

    void removeExerciseFromCourse(long exerciseId, long courseId);
}
