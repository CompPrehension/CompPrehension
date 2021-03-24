package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.ExerciseConceptEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseConceptRepository extends CrudRepository<ExerciseConceptEntity, Long> {
}
