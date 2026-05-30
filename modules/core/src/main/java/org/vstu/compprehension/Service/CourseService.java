package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.dto.course.CourseDto;
import org.vstu.compprehension.models.businesslogic.lti.LtiContext;
import org.vstu.compprehension.models.businesslogic.lti.LtiCourseContext;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkId;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.ExerciseCourseLinkRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;

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

    @Transactional(readOnly = true)
    public Optional<CourseEntity> findByExternalIdAndResourceId(String externalCourseId, Long educationResourceId) {
        return courseRepository.findByExternalCourseIdAndEducationResourceId(externalCourseId, educationResourceId);
    }

    /**
     * Recovers the trainer course id from the current LTI context. Lookup-only: the course and
     * its education resource are already created during the LTI launch, so the attempt path must
     * not have side effects. Returns empty when the context carries no course or none is found.
     */
    @Transactional(readOnly = true)
    public Optional<Long> resolveCourseIdFromLtiContext(LtiContext ctx) {
        LtiCourseContext ltiCourse = ctx.course();
        if (ltiCourse == null || ltiCourse.courseId() == null) {
            return Optional.empty();
        }
        return courseRepository.findIdByExternalCourseIdAndResourceUrlAndType(
                ltiCourse.courseId(), ctx.lmsUrl(), ctx.lmsType());
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
    public ExerciseCourseLinkEntity findExerciseCourseLinkOrThrow(long exerciseId, long courseId) {
        return findExerciseCourseLink(exerciseId, courseId).orElseThrow(() -> new IllegalStateException(String.format(
                "There is no relation between the course (id=%s) and the exercise (id=%s)", courseId, exerciseId
        )));
    }

    @Transactional(readOnly = true)
    public List<CourseDto> getUserCourses(UserEntity user) {
        if (authService.isGlobalAdmin(user.getId())) {
            return courseRepository.findAllCourseDtos();
        }
        return courseRepository.findCourseDtosByUserId(user.getId());
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

