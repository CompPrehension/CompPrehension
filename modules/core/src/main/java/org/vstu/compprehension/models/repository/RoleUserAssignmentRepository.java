package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.role.RoleUserAssignmentEntity;

@Repository
public interface RoleUserAssignmentRepository extends CrudRepository<RoleUserAssignmentEntity, Long> {

    boolean existsByUserIdAndRoleIdAndPermissionScopeId(long userId, long roleId, long permissionScopeId);

    @Query(value = "SELECT CASE WHEN COUNT(rua) > 0 THEN TRUE ELSE FALSE END " +
            "FROM RoleUserAssignmentEntity rua " +
            "JOIN rua.role r " +
            "JOIN r.permissions p " +
            "JOIN rua.permissionScope ps " +
            "WHERE rua.user.id = :userId " +
            "AND p.name = :permissionName " +
            "AND ps.kind = :permissionScopeKind " +
            "AND (:ownerId IS NULL OR ps.ownerId = :ownerId)")
    boolean isUserAuthorized(@Param("userId") long userId,
                             @Param("permissionName") String permissionName,
                             @Param("permissionScopeKind") PermissionScopeKind permissionScopeKind,
                             @Param("ownerId") Long ownerId);

    @Query(value = "SELECT CASE WHEN COUNT(rua) > 0 THEN TRUE ELSE FALSE END " +
            "FROM RoleUserAssignmentEntity rua " +
            "JOIN rua.role r " +
            "JOIN r.permissions p " +
            "JOIN rua.permissionScope ps " +
            "WHERE rua.user.id = :userId " +
            "AND p.name = :permissionName " +
            "AND ( (ps.kind = 'GLOBAL') OR (ps.kind = 'COURSE' AND ps.ownerId = :courseId) )")
    boolean isUserAuthorizedForCourseOrGlobal(@Param("userId") long userId,
                                              @Param("permissionName") String permissionName,
                                              @Param("courseId") Long courseId);
}
