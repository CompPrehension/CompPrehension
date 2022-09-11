package org.vstu.compprehension.models.repository;


import org.vstu.compprehension.models.entities.DomainEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainRepository extends CrudRepository<DomainEntity, String> {
}
