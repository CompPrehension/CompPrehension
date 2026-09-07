package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.dto.course.CourseDto;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.businesslogic.lti.LtiContext;
import org.vstu.compprehension.businesslogic.lti.LtiCourseContext;
import org.vstu.compprehension.data.cource.CourseExerciseData;
import org.vstu.compprehension.data.cource.CourseSummaryData;
import org.vstu.compprehension.businesslogic.auth.PermissionScopeKind;
import org.vstu.compprehension.repositories.data.CourseDataRepository;
import org.vstu.compprehension.repositories.data.ExerciseDataRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class CourseService {

    private final CourseDataRepository courses;
    private final ExerciseDataRepository exercises;
    private final AuthService authService;
    private final AuthScopeFactory authScopes;

    /** Идентификатор курса по внешнему id и образовательному ресурсу. */
    @Transactional(readOnly = true)
    public @NotNull Optional<Long> findCourseIdByExternalIdAndResourceId(
            @NotNull String externalCourseId, long educationResourceId) {
        return courses.findIdByExternalId(externalCourseId, educationResourceId);
    }

    /**
     * Идентификатор курса из LTI-контекста в рамках уже разрешённого образовательного
     * ресурса: ищет по {@code (externalContextId, educationResourceId)}, создаёт при
     * отсутствии (имя — из контекста, иначе {@code "id_<externalId>"}).
     *
     * @return пусто, когда в контексте нет курса
     */
    @Transactional
    public @NotNull Optional<Long> resolveOrCreateIdFromLtiContext(
            @NotNull LtiContext ctx, long educationResourceId) {
        LtiCourseContext ltiCourse = ctx.course();
        if (ltiCourse == null || ltiCourse.courseId() == null) {
            return Optional.empty();
        }
        String externalCourseId = ltiCourse.courseId();
        String courseName = ltiCourse.courseName() != null
                ? ltiCourse.courseName()
                : String.format("id_%s", externalCourseId);
        return Optional.of(courses.findIdByExternalId(externalCourseId, educationResourceId)
                .orElseGet(() -> courses.createIfAbsentAndGetId(
                        externalCourseId, courseName, educationResourceId)));
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

    /**
     * Упражнения курса из перечисленных id.
     *
     * @throws IllegalArgumentException если хотя бы одного упражнения нет в курсе
     */
    @Transactional(readOnly = true)
    public @NotNull List<CourseExerciseData> getExercisesInCourseOrThrow(
            long courseId, @NotNull Collection<Long> exerciseIds) {
        var found = courses.findExercisesInCourse(courseId, exerciseIds);
        if (found.size() != Set.copyOf(exerciseIds).size()) {
            var foundIds = found.stream().map(CourseExerciseData::exerciseId).collect(Collectors.toSet());
            var missing = exerciseIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new IllegalArgumentException(String.format(
                    "Exercises %s are not in course %s", missing, courseId));
        }
        return found;
    }

    /**
     * Упражнение показано в курсе.
     *
     * @throws IllegalStateException если связи нет
     */
    @Transactional(readOnly = true)
    public void ensureExerciseInCourse(long exerciseId, long courseId) {
        if (!courses.isExerciseInCourse(exerciseId, courseId)) {
            throw new IllegalStateException(String.format(
                    "There is no relation between the course (id=%s) and the exercise (id=%s)",
                    courseId, exerciseId));
        }
    }

    /**
     * Курсы, которые пользователю разрешено видеть.
     * <p>
     * Право в образовательном ресурсе действует во всех его курсах сразу, поэтому
     * к явно разрешённым курсам добавляются все курсы разрешённых ресурсов.
     */
    @Transactional(readOnly = true)
    public @NotNull List<CourseDto> getUserCourses(long userId) {
        if (authService.isAuthorized(userId, SystemPermission.VIEW_COURSE, authScopes.global())) {
            return toCourseDtos(courses.findAllSummaries());
        }

        var courseIds = new HashSet<>(authService.findScopeItemIdsWithPermission(
                userId, SystemPermission.VIEW_COURSE, PermissionScopeKind.COURSE));

        var educationResourceIds = authService.findScopeItemIdsWithPermission(
                userId, SystemPermission.VIEW_COURSE, PermissionScopeKind.EDUCATION_RESOURCE);
        courseIds.addAll(courses.findIdsByEducationResourceIds(educationResourceIds));

        return toCourseDtos(courses.findSummariesByIds(courseIds));
    }

    @Transactional(readOnly = true)
    public @NotNull List<CourseDto> getExerciseMemberships(long exerciseId) {
        return toCourseDtos(courses.findSummariesByExerciseId(exerciseId));
    }

    /**
     * Показать в курсе упражнение из глобального пула.
     *
     * @throws IllegalStateException если упражнение приватное — приватное принадлежит
     *                               одному курсу и в другой не переносится
     */
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
    }

    /**
     * Данные в web-контракт.
     * <p>
     * Форма ответа API — забота сервиса: иначе изменение контракта фронта заставляло бы
     * править слой доступа к данным.
     */
    private static @NotNull List<CourseDto> toCourseDtos(@NotNull List<CourseSummaryData> summaries) {
        return summaries.stream()
                .map(c -> new CourseDto(c.id(), c.name(), c.educationResourceId(), c.educationResourceUrl()))
                .toList();
    }
}
