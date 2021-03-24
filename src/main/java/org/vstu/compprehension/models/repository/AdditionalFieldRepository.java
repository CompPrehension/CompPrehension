package org.vstu.compprehension.models.repository;


import org.vstu.compprehension.models.entities.AdditionalFieldEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdditionalFieldRepository extends CrudRepository<AdditionalFieldEntity, Long> {
}
