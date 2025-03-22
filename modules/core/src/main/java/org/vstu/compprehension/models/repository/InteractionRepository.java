package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.InteractionEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InteractionRepository extends CrudRepository<InteractionEntity, Long> {
}
