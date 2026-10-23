package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.BktDomainDataEntity;

@Repository
public interface BktDomainDataRepository extends JpaRepository<BktDomainDataEntity, String> {

}
