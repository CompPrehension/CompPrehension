package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.vstu.compprehension.dto.course.CourseDto;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkId;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.ExerciseCourseLinkRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class CourseService {

    private final CourseRepository courseRepository;
    private final ExerciseCourseLinkRepository exerciseCourseLinkRepository;
    private final ExerciseRepository exerciseRepository;

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
    public List<CourseDto> getExerciseMemberships(long exerciseId) {
        return exerciseCourseLinkRepository.findAllByExerciseId(exerciseId).stream()
                .map(link -> {
                    var course = link.getCourse();
                    var er = course.getEducationResource();
                    return new CourseDto(course.getId(), course.getName(), er.getId(), er.getUrl());
                })
                .toList();
    }

    @Transactional
    public void addExerciseToCourse(long exerciseId, long courseId) {
        var exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "exercise not found"));
        if (!exercise.isPublic()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "source_not_in_global_pool");
        }
        linkExerciseWithCourseIfMissing(exerciseId, courseId);
    }

    @Transactional
    public void removeExerciseFromCourse(long exerciseId, long courseId) {
        exerciseCourseLinkRepository.deleteByExerciseIdAndCourseId(exerciseId, courseId);
    }
}

