package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.ExerciseLawsEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseLawsRepository extends CrudRepository<ExerciseLawsEntity, Long> {
}
