package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.frontend.dto.course.CourseDto;
import org.vstu.compprehension.data.cource.CourseExerciseData;
import org.vstu.compprehension.data.cource.CourseSummaryData;
import org.vstu.compprehension.data.cource.CreateCourseData;
import org.vstu.compprehension.repositories.data.CourseDataRepository;
import org.vstu.compprehension.repositories.data.ExerciseDataRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
class CourseDataServiceImpl implements CourseDataService {

    private final CourseDataRepository courses;
    private final ExerciseDataRepository exercises;

    @Transactional(readOnly = true)
    public @NotNull Optional<Long> findCourseIdByExternalIdAndResourceId(@NotNull String externalCourseId, long educationResourceId) {
        return courses.findIdByExternalId(externalCourseId, educationResourceId);
    }

    @Transactional
    public long getOrCreate(@NotNull CreateCourseData course) {
        String courseName = course.name() != null
                ? course.name()
                : String.format("id_%s", course.externalCourseId());
        return courses.findIdByExternalId(course.externalCourseId(), course.educationResourceId())
                .orElseGet(() -> courses.createIfAbsentAndGetId(
                        course.externalCourseId(), courseName, course.educationResourceId()));
    }

    @Transactional
    public void linkExerciseWithCourseIfMissing(long exerciseId, long courseId) {
        if (courses.linkExerciseIfAbsent(exerciseId, courseId)) {
            log.info("Linked exercise {} to course {}", exerciseId, courseId);
        } else {
            log.debug("Exercise {} already linked to course {}, skipping", exerciseId, courseId);
        }
    }

    @Transactional(readOnly = true)
    public @NotNull List<Long> findCourseIdsByExerciseId(long exerciseId) {
        return courses.findCourseIdsByExerciseId(exerciseId);
    }

    @Transactional(readOnly = true)
    public @NotNull List<CourseExerciseData> getExercisesInCourseOrThrow(long courseId, @NotNull Collection<Long> exerciseIds) {
        var found = courses.findExercisesInCourse(courseId, exerciseIds);
        if (found.size() != Set.copyOf(exerciseIds).size()) {
            var foundIds = found.stream().map(CourseExerciseData::exerciseId).collect(Collectors.toSet());
            var missing = exerciseIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new IllegalArgumentException(String.format(
                    "Exercises %s are not in course %s", missing, courseId));
        }
        return found;
    }

    @Transactional(readOnly = true)
    public @NotNull List<CourseSummaryData> getAllCourses() {
        return courses.findAllSummaries();
    }

    @Transactional(readOnly = true)
    public @NotNull List<CourseSummaryData> getCoursesByIds(@NotNull Collection<Long> courseIds) {
        return courses.findSummariesByIds(courseIds);
    }

    @Transactional(readOnly = true)
    public @NotNull List<Long> findCourseIdsByEducationResourceIds(@NotNull Collection<Long> educationResourceIds) {
        return courses.findIdsByEducationResourceIds(educationResourceIds);
    }

    @Transactional(readOnly = true)
    public @NotNull List<CourseSummaryData> getExerciseMemberships(long exerciseId) {
        return courses.findSummariesByExerciseId(exerciseId);
    }

    @Transactional
    public void addExerciseToCourse(long exerciseId, long courseId) {
        if (!exercises.getById(exerciseId).isPublic()) {
            throw new IllegalStateException("source_not_in_global_pool");
        }
        linkExerciseWithCourseIfMissing(exerciseId, courseId);
    }

    @Transactional
    public void removeExerciseFromCourse(long exerciseId, long courseId) {
        courses.unlinkExercise(exerciseId, courseId);
        // Приватное упражнение без курсов недостижимо, но пока оно привязано к другим курсам, удалять его нельзя.
        if (!exercises.getById(exerciseId).isPublic() && courses.findCourseIdsByExerciseId(exerciseId).isEmpty()) {
            exercises.delete(exerciseId);
        }
    }
}
