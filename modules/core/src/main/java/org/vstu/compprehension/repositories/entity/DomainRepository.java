package org.vstu.compprehension.repositories.entity;


import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.DomainEntity;

import java.util.List;

@Repository
public interface DomainRepository extends JpaRepository<DomainEntity, String> {
    @NotNull
    List<DomainEntity> findAll();
}
