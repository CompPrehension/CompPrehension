package org.vstu.compprehension.models.repository;


import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.DomainEntity;

import java.util.List;

@Repository
public interface DomainRepository extends CrudRepository<DomainEntity, String> {
    @NotNull
    List<DomainEntity> findAll();
}
