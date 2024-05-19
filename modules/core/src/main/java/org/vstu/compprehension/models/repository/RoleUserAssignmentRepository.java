package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.role.PermissionScopeEntity;
import org.vstu.compprehension.models.entities.role.RoleUserAssignmentEntity;

import java.util.List;

@Repository
public interface RoleUserAssignmentRepository extends CrudRepository<RoleUserAssignmentEntity, Long> {

    List<RoleUserAssignmentEntity> findAllByUserAndPermissionScope(UserEntity user, PermissionScopeEntity permissionScope);
}
