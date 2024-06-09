package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.EducationResourceEntity;

import java.util.Optional;

@Repository
public interface CourseRepository extends CrudRepository<CourseEntity, Long> {

    Optional<CourseEntity> findByName(String name);

    Optional<CourseEntity> findByNameAndEducationResources(String name, EducationResourceEntity educationResources);

    @Query("SELECT c.id " +
            "FROM CourseEntity c " +
            "JOIN c.exercises e " +
            "JOIN e.exerciseAttempts ea " +
            "JOIN ea.questions q " +
            "WHERE q.id = :questionId")
    Optional<Long> findCourseIdByQuestionId(@Param("questionId") Long questionId);

    @Query("SELECT c.id " +
            "FROM CourseEntity c " +
            "JOIN c.exercises e " +
            "WHERE e.id = :exerciseId")
    Optional<Long> findCourseIdByExerciseId(@Param("exerciseId") Long exerciseId);

    @Query("SELECT c.id " +
            "FROM CourseEntity c " +
            "JOIN c.exercises e " +
            "JOIN e.exerciseAttempts ea " +
            "WHERE ea.id = :attemptId")
    Optional<Long> findCourseIdByAttemptId(@Param("attemptId") Long attemptId);
}
