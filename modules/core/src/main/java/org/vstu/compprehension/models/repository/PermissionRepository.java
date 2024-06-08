package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.role.PermissionEntity;

import java.util.Optional;

@Repository
public interface PermissionRepository extends CrudRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByName(String name);
}
