package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.role.RoleUserAssignmentEntity;

import java.util.List;

@Repository
public interface RoleUserAssignmentRepository extends CrudRepository<RoleUserAssignmentEntity, Long> {

    boolean existsByUserIdAndRoleIdAndPermissionScopeId(long userId, long roleId, long permissionScopeId);

    @Query("select rua from RoleUserAssignmentEntity rua where rua.user.id = :userId and rua.permissionScope.id = :permissionScopeId")
    List<RoleUserAssignmentEntity> findUsersWithRoleOnPermissionScope(@Param("userId") long userId,
                                                                      @Param("permissionScopeId") long permissionScopeId);

    @Query(value = "SELECT EXISTS (" +
            "SELECT 1 " +
            "FROM RoleUserAssignmentEntity rua " +
            "JOIN rua.role r " +
            "JOIN r.permissions p " +
            "JOIN rua.permissionScope ps " +
            "WHERE rua.user.id = :userId " +
            "AND p.name = :permissionName " +
            "AND ( ( (ps.kind = :permissionScopeKind) AND (ps.ownerId = :ownerId) ) OR (ps.kind = 'GLOBAL') )" +
            ")")
    boolean isUserAuthorized(@Param("userId") long userId,
                             @Param("permissionName") String permissionName,
                             @Param("permissionScopeKind") PermissionScopeKind permissionScopeKind,
                             @Param("ownerId") Long ownerId);
}
