package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.exercise.ExerciseLawEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseLawsRepository extends CrudRepository<ExerciseLawEntity, Long> {
}
