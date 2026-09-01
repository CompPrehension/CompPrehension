package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.businesslogic.auth.Permission;
import org.vstu.compprehension.models.businesslogic.auth.Role;
import org.vstu.compprehension.models.entities.role.RoleUserAssignmentEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface RoleUserAssignmentRepository extends JpaRepository<RoleUserAssignmentEntity, Long> {

    @Query(value = """
            select exists(
                select 1
                from role_user_assignment rua
                join role r              on r.id = rua.role_id
                join permission_scope ps on ps.id = rua.permission_scope_id
                where rua.user_id = :userId
                  and ps.kind = 'GLOBAL'
                  and r.name = 'GLOBAL_ADMIN'

                union all

                select 1
                from role_user_assignment rua
                join role r              on r.id = rua.role_id
                join role_permission rp  on rp.role_id = r.id
                join permission p        on p.id = rp.permission_id
                join permission_scope ps on ps.id = rua.permission_scope_id
                where rua.user_id = :userId
                  and p.name = :permissionName
                  and concat(ps.kind, ':', coalesce(ps.scope_item_id, 0)) in (:scopeKeys)
            )
            """, nativeQuery = true)
    long isAuthorizedInAnyScope(
            @Param("userId") long userId,
            @Param("permissionName") String permissionName,
            @Param("scopeKeys") Collection<String> scopeKeys
    );

    @Query(value = """
            select distinct p.name
            from role_user_assignment rua
            join role r              on r.id = rua.role_id
            join role_permission rp  on rp.role_id = r.id
            join permission p        on p.id = rp.permission_id
            join permission_scope ps on ps.id = rua.permission_scope_id
            where rua.user_id = :userId
              and concat(ps.kind, ':', coalesce(ps.scope_item_id, 0)) in (:scopeKeys)

            union

            select p.name
            from permission p
            where exists (
                select 1
                from role_user_assignment rua
                join role r              on r.id = rua.role_id
                join permission_scope ps on ps.id = rua.permission_scope_id
                where rua.user_id = :userId
                  and ps.kind = 'GLOBAL'
                  and r.name = 'GLOBAL_ADMIN'
            )
            """, nativeQuery = true)
    List<String> findPermissionIdsInAnyScope(
            @Param("userId") long userId,
            @Param("scopeKeys") Collection<String> scopeKeys
    );

    @Query("""
            select exists (
                select 1
                from RoleUserAssignmentEntity rua
                join rua.role r
                join rua.permissionScope ps
                where rua.user.id = :userId
                  and r.name = :role
                  and ps.kind = :kind
                  and coalesce(ps.scopeItemId, 0) = coalesce(:scopeItemId, 0)
            )
            """)
    boolean existsRoleInScope(
            @Param("userId") long userId,
            @Param("role") Role role,
            @Param("kind") PermissionScopeKind kind,
            @Param("scopeItemId") Long scopeItemId
    );

    @Modifying
    @Query(value = """
            insert ignore into role_user_assignment (user_id, role_id, permission_scope_id)
            select :userId, r.id, ps.id
            from role r, permission_scope ps
            where r.name = :roleName
              and ps.kind = :kind
              and coalesce(ps.scope_item_id, 0) = coalesce(:scopeItemId, 0)
            """, nativeQuery = true)
    int createIfAbsent(
            @Param("userId") long userId,
            @Param("roleName") String roleName,
            @Param("kind") String kind,
            @Param("scopeItemId") Long scopeItemId
    );

    @Modifying
    @Query("""
            delete from RoleUserAssignmentEntity rua
            where rua.user.id = :userId
              and rua.role.id = (select r.id from RoleEntity r where r.name = :role)
              and rua.permissionScope.id in (
                  select ps.id from PermissionScopeEntity ps
                  where ps.kind = :kind
                    and coalesce(ps.scopeItemId, 0) = coalesce(:scopeItemId, 0))
            """)
    int deleteRoleInScope(
            @Param("userId") long userId,
            @Param("role") Role role,
            @Param("kind") PermissionScopeKind kind,
            @Param("scopeItemId") Long scopeItemId
    );

    @Modifying
    @Query("""
            delete from RoleUserAssignmentEntity rua
            where rua.user.id = :userId
              and rua.role.id not in (select r.id from RoleEntity r where r.name = :keepRole)
              and rua.permissionScope.id in (
                  select ps.id from PermissionScopeEntity ps
                  where ps.kind = :kind
                    and coalesce(ps.scopeItemId, 0) = coalesce(:scopeItemId, 0))
            """)
    int deleteRolesInScopeExcept(
            @Param("userId") long userId,
            @Param("keepRole") Role keepRole,
            @Param("kind") PermissionScopeKind kind,
            @Param("scopeItemId") Long scopeItemId
    );

    @Query("""
            select rua from RoleUserAssignmentEntity rua
            join fetch rua.user u
            join fetch rua.role r
            join fetch rua.permissionScope ps
            where ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.COURSE
              and u.id in :userIds
              and ps.scopeItemId in (
                  select c.id from CourseEntity c where c.educationResource.id = :eduResId)
            """)
    List<RoleUserAssignmentEntity> findCourseAssignmentsInEducationResource(
            @Param("eduResId") Long eduResId,
            @Param("userIds") Collection<Long> userIds
    );

    @Query("""
            select distinct ps.scopeItemId
            from RoleUserAssignmentEntity rua
            join rua.role r
            join r.permissions p
            join rua.permissionScope ps
            where rua.user.id = :userId
              and ps.kind = :kind
              and p.name = :permission
            """)
    List<Long> findScopeItemIdsWithPermission(
            @Param("userId") long userId,
            @Param("permission") Permission permission,
            @Param("kind") PermissionScopeKind kind
    );
}
