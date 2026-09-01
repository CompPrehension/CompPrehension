package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.dto.course.CourseDto;
import org.vstu.compprehension.models.businesslogic.lti.LtiContext;
import org.vstu.compprehension.models.businesslogic.lti.LtiCourseContext;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkId;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.ExerciseCourseLinkRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class CourseService {

    private final CourseRepository courseRepository;
    private final ExerciseCourseLinkRepository exerciseCourseLinkRepository;
    private final ExerciseRepository exerciseRepository;
    private final AuthService authService;
    private final AuthScopeFactory authScopes;

    @Transactional(readOnly = true)
    public Optional<CourseEntity> findByExternalIdAndResourceId(String externalCourseId, Long educationResourceId) {
        return courseRepository.findByExternalCourseIdAndEducationResourceId(externalCourseId, educationResourceId);
    }

    @Transactional
    public CourseEntity createOrGetExisting(
            String externalCourseId,
            String name,
            Long educationResourceId
    ) {
        courseRepository.createIfAbsent(externalCourseId, name, educationResourceId);
        return courseRepository.findByExternalCourseIdAndEducationResourceId(externalCourseId, educationResourceId)
                .orElseThrow(() -> new IllegalStateException("createIfAbsent: course not found after insert"));
    }

    /**
     * Курс из LTI-контекста в рамках уже разрешённого education resource: ищет по
     * {@code (externalContextId, educationResourceId)}, создаёт при отсутствии (имя — из контекста,
     * fallback {@code "id_<externalId>"}). Возвращает {@code null}, когда в контексте нет курса.
     */
    @Transactional
    public CourseEntity resolveOrCreateFromLtiContext(LtiContext ctx, Long educationResourceId) {
        LtiCourseContext ltiCourse = ctx.course();
        if (ltiCourse == null || ltiCourse.courseId() == null) {
            return null;
        }
        String externalCourseId = ltiCourse.courseId();
        String courseName = ltiCourse.courseName() != null
            ? ltiCourse.courseName()
                : String.format("id_%s", externalCourseId);
        return findByExternalIdAndResourceId(externalCourseId, educationResourceId)
                .orElseGet(() -> createOrGetExisting(externalCourseId, courseName, educationResourceId));
    }

    @Transactional
    public void linkExerciseWithCourseIfMissing(long exerciseId, long courseId) {
        int affectedRows = exerciseCourseLinkRepository.createIfAbsent(exerciseId, courseId);
        if (affectedRows > 0) {
            log.info("Linked exercise {} to course {}", exerciseId, courseId);
        } else {
            log.debug("Exercise {} already linked to course {}, skipping", exerciseId, courseId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<ExerciseCourseLinkEntity> findExerciseCourseLink(long exerciseId, long courseId) {
        return exerciseCourseLinkRepository.findById(new ExerciseCourseLinkId(exerciseId, courseId));
    }

    @Transactional(readOnly = true)
    public List<Long> findCourseIdsByExerciseId(long exerciseId) {
        return exerciseCourseLinkRepository.findAllByExerciseId(exerciseId).stream()
                .map(link -> link.getCourse().getId())
                .toList();
    }

    @Transactional(readOnly = true)
    public ExerciseCourseLinkEntity findExerciseCourseLinkOrThrow(long exerciseId, long courseId) {
        return findExerciseCourseLink(exerciseId, courseId).orElseThrow(() -> new IllegalStateException(String.format(
                "There is no relation between the course (id=%s) and the exercise (id=%s)", courseId, exerciseId
        )));
    }

    @Transactional(readOnly = true)
    public List<CourseDto> getUserCourses(UserEntity user) {
        long userId = user.getId();
        if (authService.isAuthorized(userId, SystemPermission.VIEW_COURSE, authScopes.global())) {
            return courseRepository.findAllCourseDtos();
        }

        var courseIds = new HashSet<>(
                authService.findScopeItemIdsWithPermission(userId, SystemPermission.VIEW_COURSE, PermissionScopeKind.COURSE));

        // Право в образовательном ресурсе действует во всех его курсах сразу.
        var educationResourceIds =
                authService.findScopeItemIdsWithPermission(userId, SystemPermission.VIEW_COURSE, PermissionScopeKind.EDUCATION_RESOURCE);
        if (!educationResourceIds.isEmpty()) {
            courseIds.addAll(courseRepository.findCourseIdsByEducationResourceIdIn(educationResourceIds));
        }

        return courseIds.isEmpty() ? List.of() : courseRepository.findCourseDtosByIdIn(courseIds);
    }

    @Transactional(readOnly = true)
    public List<CourseDto> getExerciseMemberships(long exerciseId) {
        return exerciseCourseLinkRepository.findCourseDtosByExerciseId(exerciseId);
    }

    @Transactional
    public void addExerciseToCourse(long exerciseId, long courseId) {
        var exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NoSuchElementException("exercise not found"));
        if (!exercise.isPublic()) {
            throw new IllegalStateException("source_not_in_global_pool");
        }
        linkExerciseWithCourseIfMissing(exerciseId, courseId);
    }

    @Transactional
    public void removeExerciseFromCourse(long exerciseId, long courseId) {
        exerciseCourseLinkRepository.deleteByExerciseIdAndCourseId(exerciseId, courseId);
    }
}
