package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.ExerciseDisplayingFeedbackTypeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseDisplayingFeedbackTypeRepository extends CrudRepository<ExerciseDisplayingFeedbackTypeEntity, Long> {
}
