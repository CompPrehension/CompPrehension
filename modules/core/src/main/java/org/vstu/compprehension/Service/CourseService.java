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

    /**
     * @return пара (связь exercise-course, true если связь была создана / false если уже существовала)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<ExerciseCourseEntity, Boolean> linkExerciseWithCourseIfMissing(Long exerciseId, Long courseId) {
        if (exerciseId == null || courseId == null) {
            throw new IllegalArgumentException("exerciseId and courseId must be non-null");
        }
        var existing = findExerciseCourseRelation(exerciseId, courseId);
        if (existing.isPresent()) {
            return Pair.of(existing.get(), false);
        }
        try {
            ExerciseCourseEntity saved = exerciseCourseRepository.saveAndFlush(new ExerciseCourseEntity(
                    courseRepository.getReferenceById(courseId),
                    exerciseRepository.getReferenceById(exerciseId)
            ));
            log.info("Linked exercise {} to course {}", exerciseId, courseId);
            return Pair.of(saved, true);
        } catch (JpaObjectRetrievalFailureException e) {
            // getReferenceById вернул прокси на несуществующую сущность;
            // сообщение cause содержит имя класса и id (Hibernate: "Unable to find XxxEntity with id N")
            throw new IllegalArgumentException(String.format(
                    "Cannot link exercise %d with course %d: entity not found - %s",
                    exerciseId, courseId, e.getCause() != null ? e.getCause().getMessage() : e.getMessage()
            ), e);
        } catch (DataIntegrityViolationException e) {
            return Pair.of(
                    findExerciseCourseRelation(exerciseId, courseId)
                            .orElseThrow(() -> new IllegalStateException(String.format(
                                    "Failed to link exercise %d with course %d", exerciseId, courseId), e)
                            ),
                    false
            );
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
