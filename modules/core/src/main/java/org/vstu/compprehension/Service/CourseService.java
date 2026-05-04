package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkId;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.ExerciseCourseLinkRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class CourseService {

    private final CourseRepository courseRepository;
    private final ExerciseCourseLinkRepository exerciseCourseLinkRepository;

    @Transactional(readOnly = true)
    public Optional<CourseEntity> findByExternalIdAndResourceId(String externalCourseId, Long educationResourceId) {
        return courseRepository.findByExternalCourseIdAndEducationResourceId(externalCourseId, educationResourceId);
    }

    @Transactional
    public CourseEntity createOrGetExisting(CourseEntity course) {
        courseRepository.createIfAbsent(course);
        return courseRepository.findByExternalCourseIdAndEducationResourceId(
                        course.getExternalCourseId(), course.getEducationResource().getId())
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
}

