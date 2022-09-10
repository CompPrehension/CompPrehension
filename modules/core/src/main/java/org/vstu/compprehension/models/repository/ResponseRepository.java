package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.ResponseEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseRepository extends CrudRepository<ResponseEntity, Long> {
}
