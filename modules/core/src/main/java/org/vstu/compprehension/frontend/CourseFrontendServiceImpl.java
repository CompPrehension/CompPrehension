package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.lti.LtiContext;
import org.vstu.compprehension.frontend.dto.ExerciseRefDto;
import org.vstu.compprehension.frontend.dto.course.CourseDto;
import org.vstu.compprehension.services.CourseDataService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class CourseFrontendServiceImpl implements CourseFrontendService {
    private final CourseDataService courseService;

    public CourseFrontendServiceImpl(CourseDataService courseService) {
        this.courseService = courseService;
    }

    @Override
    public @NotNull List<CourseDto> getUserCourses(long userId) {
        return courseService.getUserCourses(userId);
    }

    @Override
    public @NotNull List<CourseDto> getExerciseMemberships(long exerciseId) {
        return courseService.getExerciseMemberships(exerciseId);
    }

    @Override
    public void addExerciseToCourse(long exerciseId, long courseId) {
        courseService.addExerciseToCourse(exerciseId, courseId);
    }

    @Override
    public void removeExerciseFromCourse(long exerciseId, long courseId) {
        courseService.removeExerciseFromCourse(exerciseId, courseId);
    }

    @Override
    public void linkExerciseWithCourseIfMissing(long exerciseId, long courseId) {
        courseService.linkExerciseWithCourseIfMissing(exerciseId, courseId);
    }

    @Override
    public @NotNull Optional<Long> resolveOrCreateIdFromLtiContext(@NotNull LtiContext ctx, long educationResourceId) {
        return courseService.resolveOrCreateIdFromLtiContext(ctx, educationResourceId);
    }

    @Override
    public @NotNull Optional<Long> findCourseIdByExternalIdAndResourceId(@NotNull String externalCourseId, long educationResourceId) {
        return courseService.findCourseIdByExternalIdAndResourceId(externalCourseId, educationResourceId);
    }

    @Override
    public @NotNull List<ExerciseRefDto> getExerciseRefsInCourseOrThrow(long courseId, @NotNull Collection<Long> exerciseIds) {
        return courseService.getExercisesInCourseOrThrow(courseId, exerciseIds).stream()
                .map(ref -> new ExerciseRefDto(ref.exerciseId(), ref.name()))
                .toList();
    }
}
