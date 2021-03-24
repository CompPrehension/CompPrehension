package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.UserActionExerciseEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActionExerciseRepository extends CrudRepository<UserActionExerciseEntity, Long> {
}
