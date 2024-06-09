package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.entities.course.CourseEntity;

public interface CourseService {
    CourseEntity getCurrentCourse() throws Exception;
    long getInitialCourseId();
    CourseEntity getOrCreateCourse(String name, Long educationResourceId);

    Long getCourseIdFromQuestion(Long questionId);

    Long getCourseIdFromAttempt(Long attemptId);

    Long getCourseIdFromExercise(Long exerciseId);
}
