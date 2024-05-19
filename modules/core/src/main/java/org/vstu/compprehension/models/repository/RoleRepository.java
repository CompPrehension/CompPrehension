package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.role.RoleEntity;

import java.util.Optional;

@Repository
public interface RoleRepository extends CrudRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(String name);
}
