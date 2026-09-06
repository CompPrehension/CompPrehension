package org.vstu.compprehension.models.repository.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.data.CourseEducationResourceData;
import org.vstu.compprehension.models.data.CourseExerciseData;
import org.vstu.compprehension.models.data.CourseSummaryData;
import org.vstu.compprehension.models.data.ExternalCourseData;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkId;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.CourseRepository.CourseEducationResourceView;
import org.vstu.compprehension.models.repository.CourseRepository.CourseView;
import org.vstu.compprehension.models.repository.ExerciseCourseLinkRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Курсы и состав их упражнений.
 * <p>
 * Одно место, потому что курс без списка упражнений и упражнение без курсов, в которых
 * оно показано, — это две половины одного вопроса: связь {@code exercise_course_link}
 * читается и с той и с другой стороны, и оба раза нужна одна и та же осторожность с
 * ленивыми концами.
 * <p>
 * Сам {@code CourseEntity} наружу не выходит ни разу: вызывающим нужен либо идентификатор
 * курса, либо его карточка для списка, и обе формы объявлены отдельными типами.
 */
@Repository
@RequiredArgsConstructor
public class CourseDataRepository {

    private final CourseRepository courseRepository;
    private final ExerciseCourseLinkRepository exerciseCourseLinkRepository;

    // ---------------------------------------------------------------- курсы

    /** Все курсы системы. */
    @Transactional(readOnly = true)
    public @NotNull List<CourseSummaryData> findAllSummaries() {
        return toSummaries(courseRepository.findAllCourseViews());
    }

    /** Курсы из перечисленных; отсутствующие просто не попадают в ответ. */
    @Transactional(readOnly = true)
    public @NotNull List<CourseSummaryData> findSummariesByIds(@NotNull Collection<Long> courseIds) {
        return courseIds.isEmpty() ? List.of() : toSummaries(courseRepository.findCourseViewsByIdIn(courseIds));
    }

    /** Курсы, в которых показано упражнение. */
    @Transactional(readOnly = true)
    public @NotNull List<CourseSummaryData> findSummariesByExerciseId(long exerciseId) {
        return toSummaries(exerciseCourseLinkRepository.findCourseViewsByExerciseId(exerciseId));
    }

    /** Курсы образовательного ресурса, заведённые по его внешним курсам. */
    @Transactional(readOnly = true)
    public @NotNull List<ExternalCourseData> findExternalCourses(long educationResourceId) {
        return courseRepository.findExternalCourses(educationResourceId).stream()
                .map(CourseDataRepository::toExternalCourse)
                .toList();
    }

    /**
     * Отвязать курсы от внешней системы: они становятся локальными.
     * <p>
     * Так уходят курсы, удалённые в LMS: синхронизация их больше не запрашивает,
     * а накопленные в них попытки и роли остаются.
     */
    @Transactional
    public void detachFromExternalSystem(@NotNull Collection<Long> courseIds) {
        if (!courseIds.isEmpty()) {
            courseRepository.detachFromExternalSystem(courseIds);
        }
    }

    /** Курс по внешнему идентификатору в образовательном ресурсе; пусто, если не заведён. */
    @Transactional(readOnly = true)
    public @NotNull Optional<Long> findIdByExternalId(@NotNull String externalCourseId, long educationResourceId) {
        return courseRepository.findByExternalCourseIdAndEducationResourceId(externalCourseId, educationResourceId)
                .map(course -> Strict.required(course.getId(), "id", "course " + externalCourseId));
    }

    /**
     * Курс по внешнему идентификатору, заводя его при отсутствии.
     * <p>
     * Вставка идемпотентная ({@code insert ignore}) и после неё идёт чтение: при
     * одновременном заходе двух студентов курс создаст кто-то один, а второй прочитает
     * уже существующую строку.
     *
     * @throws IllegalStateException если строки нет и после вставки
     */
    @Transactional
    public long createIfAbsentAndGetId(@NotNull String externalCourseId, @NotNull String name,
                                       long educationResourceId) {
        courseRepository.createIfAbsent(externalCourseId, name, educationResourceId);
        return findIdByExternalId(externalCourseId, educationResourceId)
                .orElseThrow(() -> new IllegalStateException(
                        "Course " + externalCourseId + " of education resource " + educationResourceId
                                + " not found after insert"));
    }

    /** Курсы перечисленных образовательных ресурсов. */
    @Transactional(readOnly = true)
    public @NotNull List<Long> findIdsByEducationResourceIds(@NotNull Collection<Long> educationResourceIds) {
        return educationResourceIds.isEmpty() ? List.of()
                : courseRepository.findCourseIdsByEducationResourceIdIn(educationResourceIds);
    }

    /** Образовательные ресурсы перечисленных курсов; несуществующие курсы в ответ не попадают. */
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

    // ------------------------------------------------- состав курса

    /** Курсы, в которых показано упражнение. */
    @Transactional(readOnly = true)
    public @NotNull List<Long> findCourseIdsByExerciseId(long exerciseId) {
        return exerciseCourseLinkRepository.findCourseIdsByExerciseId(exerciseId);
    }

    /** Показано ли упражнение в курсе. */
    @Transactional(readOnly = true)
    public boolean isExerciseInCourse(long exerciseId, long courseId) {
        return exerciseCourseLinkRepository.existsById(new ExerciseCourseLinkId(exerciseId, courseId));
    }

    /**
     * Упражнения курса из перечисленных.
     * <p>
     * Одним запросом на весь список: вызов по одному упражнению в цикле давал и N+1,
     * и обращение к ленивой связи уже за пределами транзакции. Отсутствующие в курсе
     * упражнения просто не попадают в ответ — решать, ошибка это или нет, вызывающему.
     */
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

    /**
     * Показать упражнение в курсе, если оно там ещё не показано.
     *
     * @return true, если связь появилась именно сейчас
     */
    @Transactional
    public boolean linkExerciseIfAbsent(long exerciseId, long courseId) {
        return exerciseCourseLinkRepository.createIfAbsent(exerciseId, courseId) > 0;
    }

    /** Убрать упражнение из курса; само упражнение остаётся. */
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

    private static @NotNull ExternalCourseData toExternalCourse(
            @NotNull CourseRepository.ExternalCourseView view) {
        long id = Strict.required(view.getId(), "id", "external course");
        return new ExternalCourseData(
                id,
                Strict.required(view.getName(), "name", "course " + id),
                Strict.required(view.getExternalCourseId(), "externalCourseId", "course " + id));
    }

    private static @NotNull CourseEducationResourceData toEducationResourceRef(
            @NotNull CourseEducationResourceView view) {
        long courseId = Strict.required(view.getCourseId(), "courseId", "course education resource ref");
        return new CourseEducationResourceData(
                courseId,
                Strict.required(view.getEducationResourceId(), "educationResourceId", "course " + courseId));
    }

    private static @NotNull CourseExerciseData toCourseExercise(@NotNull ExerciseCourseLinkEntity link) {
        ExerciseEntity exercise = Strict.required(link.getExercise(), "exercise", "exercise course link");
        long exerciseId = Strict.required(exercise.getId(), "id", "exercise of course link");
        return new CourseExerciseData(
                exerciseId,
                Strict.required(exercise.getName(), "name", "exercise " + exerciseId));
    }
}
