package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.cource.CourseEducationResourceData;
import org.vstu.compprehension.data.cource.CourseExerciseData;
import org.vstu.compprehension.data.cource.CourseSummaryData;
import org.vstu.compprehension.data.cource.ExternalCourseData;
import org.vstu.compprehension.entities.course.ExerciseCourseLinkEntity;
import org.vstu.compprehension.entities.course.ExerciseCourseLinkId;
import org.vstu.compprehension.entities.ExerciseEntity;
import org.vstu.compprehension.repositories.entity.CourseRepository;
import org.vstu.compprehension.repositories.entity.CourseRepository.CourseView;
import org.vstu.compprehension.repositories.entity.ExerciseCourseLinkRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourseDataRepository {

    private final CourseRepository courseRepository;
    private final ExerciseCourseLinkRepository exerciseCourseLinkRepository;

    @Transactional(readOnly = true)
    public @NotNull List<CourseSummaryData> findAllSummaries() {
        return toSummaries(courseRepository.findAllCourseViews());
    }

    @Transactional(readOnly = true)
    public @NotNull List<CourseSummaryData> findSummariesByIds(@NotNull Collection<Long> courseIds) {
        return courseIds.isEmpty() ? List.of() : toSummaries(courseRepository.findCourseViewsByIdIn(courseIds));
    }

    @Transactional(readOnly = true)
    public @NotNull List<CourseSummaryData> findSummariesByExerciseId(long exerciseId) {
        return toSummaries(exerciseCourseLinkRepository.findCourseViewsByExerciseId(exerciseId));
    }

    @Transactional(readOnly = true)
    public @NotNull List<ExternalCourseData> findExternalCourses(long educationResourceId) {
        return courseRepository.findExternalCourses(educationResourceId).stream()
                .map(CourseDataRepository::toExternalCourse)
                .toList();
    }

    /**
     * Отвязать курсы от внешней системы.
     */
    @Transactional
    public void detachFromExternalSystem(@NotNull Collection<Long> courseIds) {
        if (!courseIds.isEmpty()) {
            courseRepository.detachFromExternalSystem(courseIds);
        }
    }

    @Transactional(readOnly = true)
    public @NotNull Optional<Long> findIdByExternalId(@NotNull String externalCourseId, long educationResourceId) {
        return courseRepository.findByExternalCourseIdAndEducationResourceId(externalCourseId, educationResourceId)
                .map(course -> Strict.required(course.getId(), "id", "course " + externalCourseId));
    }

    @Transactional
    public long createIfAbsentAndGetId(@NotNull String externalCourseId, @NotNull String name, long educationResourceId) {
        courseRepository.createIfAbsent(externalCourseId, name, educationResourceId);
        return findIdByExternalId(externalCourseId, educationResourceId)
                .orElseThrow(() -> new IllegalStateException(
                        "Course " + externalCourseId + " of education resource " + educationResourceId
                                + " not found after insert"));
    }

    @Transactional(readOnly = true)
    public @NotNull List<Long> findIdsByEducationResourceIds(@NotNull Collection<Long> educationResourceIds) {
        return educationResourceIds.isEmpty() ? List.of()
                : courseRepository.findCourseIdsByEducationResourceIdIn(educationResourceIds);
    }

    @Transactional(readOnly = true)
    public @NotNull List<CourseEducationResourceData> findEducationResourceRefs(
            @NotNull Collection<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return courseRepository.findEducationResourceRefsByCourseIdIn(courseIds).stream()
                .map(CourseDataRepository::toEducationResourceRef)
                .toList();
    }

    @Transactional(readOnly = true)
    public @NotNull List<Long> findCourseIdsByExerciseId(long exerciseId) {
        return exerciseCourseLinkRepository.findCourseIdsByExerciseId(exerciseId);
    }

    @Transactional(readOnly = true)
    public boolean isExerciseInCourse(long exerciseId, long courseId) {
        return exerciseCourseLinkRepository.existsById(new ExerciseCourseLinkId(exerciseId, courseId));
    }

    @Transactional(readOnly = true)
    public @NotNull List<CourseExerciseData> findExercisesInCourse(long courseId,
                                                                   @NotNull Collection<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return List.of();
        }
        return exerciseCourseLinkRepository
                .findAllByCourseIdAndExerciseIdsFetchingExercise(courseId, exerciseIds).stream()
                .map(CourseDataRepository::toCourseExercise)
                .toList();
    }

    @Transactional
    public boolean linkExerciseIfAbsent(long exerciseId, long courseId) {
        return exerciseCourseLinkRepository.createIfAbsent(exerciseId, courseId) > 0;
    }

    @Transactional
    public void unlinkExercise(long exerciseId, long courseId) {
        exerciseCourseLinkRepository.deleteByExerciseIdAndCourseId(exerciseId, courseId);
    }

    // ---------------------------------------------------------------- маппинг

    private static @NotNull List<CourseSummaryData> toSummaries(@NotNull List<CourseView> views) {
        return views.stream().map(CourseDataRepository::toSummary).toList();
    }

    private static @NotNull CourseSummaryData toSummary(@NotNull CourseView view) {
        return new CourseSummaryData(
                view.getId(),
                Strict.required(view.getName(), "name", "course " + view.getId()),
                view.getEducationResourceId(),
                Strict.required(view.getEducationResourceUrl(), "educationResourceUrl",
                        "course " + view.getId()));
    }

    private static @NotNull ExternalCourseData toExternalCourse(@NotNull CourseView view) {
        long id = view.getId();
        return new ExternalCourseData(
                id,
                Strict.required(view.getName(), "name", "course " + id),
                Strict.required(view.getExternalCourseId(), "externalCourseId", "course " + id));
    }

    private static @NotNull CourseEducationResourceData toEducationResourceRef(@NotNull CourseView view) {
        long courseId = view.getId();
        return new CourseEducationResourceData(
                courseId,
                view.getEducationResourceId());
    }

    private static @NotNull CourseExerciseData toCourseExercise(@NotNull ExerciseCourseLinkEntity link) {
        ExerciseEntity exercise = Strict.required(link.getExercise(), "exercise", "exercise course link");
        long exerciseId = Strict.required(exercise.getId(), "id", "exercise of course link");
        return new CourseExerciseData(
                exerciseId,
                Strict.required(exercise.getName(), "name", "exercise " + exerciseId));
    }
}
