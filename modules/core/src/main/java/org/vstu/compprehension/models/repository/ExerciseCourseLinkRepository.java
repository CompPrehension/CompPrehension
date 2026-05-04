package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkEntity;
import org.vstu.compprehension.models.entities.course.ExerciseCourseLinkId;

@Repository
public interface ExerciseCourseLinkRepository extends JpaRepository<ExerciseCourseLinkEntity, ExerciseCourseLinkId> {
}
