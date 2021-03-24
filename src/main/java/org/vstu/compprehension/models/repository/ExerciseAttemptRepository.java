package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseAttemptRepository extends CrudRepository<ExerciseAttemptEntity, Long> {
}
