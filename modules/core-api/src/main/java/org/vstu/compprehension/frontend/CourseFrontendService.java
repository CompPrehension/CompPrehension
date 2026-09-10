package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.lti.LtiContext;
import org.vstu.compprehension.frontend.dto.ExerciseRefDto;
import org.vstu.compprehension.frontend.dto.course.CourseDto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseFrontendService {
    @NotNull List<CourseDto> getUserCourses(long userId);

    @NotNull List<CourseDto> getExerciseMemberships(long exerciseId);

    void addExerciseToCourse(long exerciseId, long courseId);

    void removeExerciseFromCourse(long exerciseId, long courseId);

    void linkExerciseWithCourseIfMissing(long exerciseId, long courseId);

    @NotNull Optional<Long> resolveOrCreateIdFromLtiContext(@NotNull LtiContext ctx, long educationResourceId);

    @NotNull Optional<Long> findCourseIdByExternalIdAndResourceId(@NotNull String externalCourseId, long educationResourceId);

    @NotNull List<ExerciseRefDto> getExerciseRefsInCourseOrThrow(long courseId, @NotNull Collection<Long> exerciseIds);
}
