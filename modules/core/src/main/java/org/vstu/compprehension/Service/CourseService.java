package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseEntity;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.ExerciseCourseRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class CourseService {

    private final CourseRepository courseRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseCourseRepository exerciseCourseRepository;

    @Transactional(readOnly = true)
    public Optional<CourseEntity> findByExternalIdAndResourceId(String externalCourseId, Long educationResourceId) {
        return courseRepository.findByExternalCourseIdAndEducationResourceId(externalCourseId, educationResourceId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CourseEntity saveOrGetExisting(CourseEntity course) {
        try {
            return courseRepository.saveAndFlush(course);
        } catch (DataIntegrityViolationException e) {
            return courseRepository.findByExternalCourseIdAndEducationResourceId(
                            course.getExternalCourseId(), course.getEducationResource().getId()
                    )
                    .orElseThrow(() -> new IllegalStateException(
                            "Failed to saveOrGetExisting Course and could not find existing one", e));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void linkExerciseWithCourseIfMissing(long exerciseId, long courseId) {
        try {
            exerciseCourseLinkRepository.saveAndFlush(new ExerciseCourseLinkEntity(
                    courseRepository.getReferenceById(courseId),
                    exerciseRepository.getReferenceById(exerciseId)
            ));
            log.info("Linked exercise {} to course {}", exerciseId, courseId);
        } catch (JpaObjectRetrievalFailureException e) {
            throw new IllegalArgumentException(String.format(
                    "Cannot link exercise %d with course %d: entity not found - %s",
                    exerciseId, courseId, e.getCause() != null ? e.getCause().getMessage() : e.getMessage()
            ), e);
        } catch (DataIntegrityViolationException e) {
            log.debug("Exercise {} already linked to course {}, skipping", exerciseId, courseId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<ExerciseCourseEntity> findExerciseCourseRelation(Long exerciseId, Long courseId) {
        if (exerciseId == null || courseId == null) {
            return Optional.empty();
        }
        return exerciseCourseRepository.findByExercise_IdAndCourse_Id(exerciseId, courseId);
    }

    @Transactional(readOnly = true)
    public void ensureExerciseBelongsToCourse(Long exerciseId, Long courseId) {
        findExerciseCourseRelation(exerciseId, courseId).orElseThrow(() -> new IllegalStateException(String.format(
                "There is no relation between the course (id=%s) and the exercise (id=%s)", courseId, exerciseId
        )));
    }
}
