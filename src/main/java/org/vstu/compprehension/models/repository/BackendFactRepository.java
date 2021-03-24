package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackendFactRepository extends CrudRepository<BackendFactEntity, Long> {
}
