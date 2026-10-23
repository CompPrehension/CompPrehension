package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.ResponseEntity;

@Repository
public interface ResponseRepository extends JpaRepository<ResponseEntity, Long> {
}
