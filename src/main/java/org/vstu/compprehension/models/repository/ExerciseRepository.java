package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.ExerciseEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseRepository extends CrudRepository<ExerciseEntity, Long> {
}
