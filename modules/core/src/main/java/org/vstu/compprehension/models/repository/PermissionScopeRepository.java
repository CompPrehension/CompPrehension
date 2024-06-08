package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.role.PermissionScopeEntity;

import java.util.Optional;

@Repository
public interface PermissionScopeRepository extends CrudRepository<PermissionScopeEntity, Long> {

    Optional<PermissionScopeEntity> findByKind(PermissionScopeKind kind);

    Optional<PermissionScopeEntity> findByOwnerIdAndKind(long ownerId, PermissionScopeKind kind);
}
