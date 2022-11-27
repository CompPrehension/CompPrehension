package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.exercise.ExerciseConceptEntity;

@Repository
public interface ExerciseConceptRepository extends CrudRepository<ExerciseConceptEntity, Long> {
}
