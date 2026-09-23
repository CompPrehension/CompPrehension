package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.frontend.dto.ExerciseRefDto;
import org.vstu.compprehension.frontend.dto.course.CourseDto;
import org.vstu.compprehension.frontend.dto.course.CreateCourseDto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseFrontendService {
    @NotNull List<CourseDto> getUserCourses(long userId);

    @NotNull List<CourseDto> getExerciseMemberships(long exerciseId);

    void addExerciseToCourse(long exerciseId, long courseId);

    void removeExerciseFromCourse(long exerciseId, long courseId);

    void linkExerciseWithCourseIfMissing(long exerciseId, long courseId);

    long getOrCreate(@NotNull CreateCourseDto course);

    @NotNull Optional<Long> findCourseIdByExternalIdAndResourceId(@NotNull String externalCourseId, long educationResourceId);

    @NotNull List<ExerciseRefDto> getExerciseRefsInCourseOrThrow(long courseId, @NotNull Collection<Long> exerciseIds);
}
