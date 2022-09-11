package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.ExerciseQuestionTypeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseQuestionTypeRepository extends CrudRepository<ExerciseQuestionTypeEntity, Long> {
}
