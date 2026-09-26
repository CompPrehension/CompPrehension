package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.frontend.dto.ExerciseRefDto;
import org.vstu.compprehension.frontend.dto.course.CourseDto;
import org.vstu.compprehension.frontend.dto.course.CreateCourseDto;
import org.vstu.compprehension.data.cource.CourseExerciseData;
import org.vstu.compprehension.data.cource.CourseSummaryData;
import org.vstu.compprehension.data.cource.CreateCourseData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.services.CourseDataService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class CourseFrontendServiceImpl implements CourseFrontendService {
    private final CourseDataService courseService;
    private final AuthFrontendService authService;
    private final Mapper<CourseSummaryData, CourseDto> courseDtoMapper;
    private final Mapper<CourseExerciseData, ExerciseRefDto> exerciseRefDtoMapper;

    public CourseFrontendServiceImpl(CourseDataService courseService,
                                     AuthFrontendService authService,
                                     Mapper<CourseSummaryData, CourseDto> courseDtoMapper,
                                     Mapper<CourseExerciseData, ExerciseRefDto> exerciseRefDtoMapper) {
        this.courseService = courseService;
        this.authService = authService;
        this.courseDtoMapper = courseDtoMapper;
        this.exerciseRefDtoMapper = exerciseRefDtoMapper;
    }

    @Override
    public @NotNull List<CourseDto> getUserCourses(long userId) {
        var courses = authService.canViewAllCourses(userId)
                ? courseService.getAllCourses()
                : courseService.getCoursesByIds(authService.findVisibleCourseIds(userId));
        return courseDtoMapper.mapAll(courses);
    }

    @Override
    public @NotNull List<CourseDto> getExerciseMemberships(long exerciseId) {
        return courseDtoMapper.mapAll(courseService.getExerciseMemberships(exerciseId));
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
    public long getOrCreate(@NotNull CreateCourseDto course) {
        return courseService.getOrCreate(new CreateCourseData(
                course.educationResourceId(), course.externalCourseId(), course.name()));
    }

    @Override
    public @NotNull Optional<Long> findCourseIdByExternalIdAndResourceId(@NotNull String externalCourseId, long educationResourceId) {
        return courseService.findCourseIdByExternalIdAndResourceId(externalCourseId, educationResourceId);
    }

    @Override
    public @NotNull List<ExerciseRefDto> getExerciseRefsInCourseOrThrow(long courseId, @NotNull Collection<Long> exerciseIds) {
        return exerciseRefDtoMapper.mapAll(
                courseService.getExercisesInCourseOrThrow(courseId, exerciseIds));
    }
}
