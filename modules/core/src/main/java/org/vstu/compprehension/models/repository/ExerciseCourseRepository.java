package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.course.ExerciseCourseEntity;

import java.util.Optional;

@Repository
public interface ExerciseCourseRepository extends JpaRepository<ExerciseCourseEntity, Long> {
    boolean existsByExercise_IdAndCourse_Id(Long exerciseId, Long courseId);
    Optional<ExerciseCourseEntity> findByExercise_IdAndCourse_Id(Long exerciseId, Long courseId);
}
