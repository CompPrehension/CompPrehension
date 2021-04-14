package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.ViolationEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ViolationRepository extends CrudRepository<ViolationEntity, Long> {
}
