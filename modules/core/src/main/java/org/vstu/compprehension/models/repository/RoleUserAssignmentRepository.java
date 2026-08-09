package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.Permission;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.Role;
import org.vstu.compprehension.models.entities.role.RoleUserAssignmentEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface RoleUserAssignmentRepository extends JpaRepository<RoleUserAssignmentEntity, Long> {

    @Query("""
            select case when count(rua) > 0 then true else false end
            from RoleUserAssignmentEntity rua
            join rua.role r
            join r.permissions p
            join rua.permissionScope ps
            where rua.user.id = :userId
              and p.name = :permission
              and (
                  (ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.GLOBAL
                      and r.name = org.vstu.compprehension.models.entities.EnumData.Role.GLOBAL_ADMIN)
            
                  or (:courseId is null
                      and :educationResourceId is null
                      and ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.GLOBAL)
            
                  or (:courseId is not null
                      and ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.COURSE
                      and ps.course.id = :courseId)
            
                  or (:courseId is not null
                      and ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.EDUCATION_RESOURCE
                      and ps.educationResource.id =
                          (select c.educationResource.id from CourseEntity c where c.id = :courseId))
            
                  or (:educationResourceId is not null
                      and ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.EDUCATION_RESOURCE
                      and ps.educationResource.id = :educationResourceId)
              )
            """)
    boolean isUserAuthorized(
            @Param("userId") long userId,
            @Param("permission") Permission permission,
            @Param("courseId") Long courseId,
            @Param("educationResourceId") Long educationResourceId
    );

    boolean existsByUser_IdAndRole_NameAndPermissionScope_Kind(long userId, Role roleName, PermissionScopeKind kind);

    @Query("""
            select distinct r.name
            from RoleUserAssignmentEntity rua
            join rua.role r
            join rua.permissionScope ps
            where rua.user.id = :userId
              and (
                  (ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.GLOBAL
                      and r.name = org.vstu.compprehension.models.entities.EnumData.Role.GLOBAL_ADMIN)
            
                  or (:courseId is null
                      and ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.GLOBAL)
            
                  or (:courseId is not null
                      and ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.COURSE
                      and ps.course.id = :courseId)
            
                  or (:courseId is not null
                      and ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.EDUCATION_RESOURCE
                      and ps.educationResource.id =
                          (select c.educationResource.id from CourseEntity c where c.id = :courseId))
              )
            """)
    List<Role> findRolesInScope(@Param("userId") long userId, @Param("courseId") Long courseId);

    @Query("""
            select case when count(rua) > 0 then true else false end
            from RoleUserAssignmentEntity rua
            join rua.role r
            join rua.permissionScope ps
            where rua.user.id = :userId
              and r.name = :role
              and (
                  (ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.GLOBAL
                      and r.name = org.vstu.compprehension.models.entities.EnumData.Role.GLOBAL_ADMIN)

                  or (:courseId is null
                      and ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.GLOBAL)

                  or (:courseId is not null
                      and ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.COURSE
                      and ps.course.id = :courseId)

                  or (:courseId is not null
                      and ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.EDUCATION_RESOURCE
                      and ps.educationResource.id =
                          (select c.educationResource.id from CourseEntity c where c.id = :courseId))
              )
            """)
    boolean existsRoleInScope(@Param("userId") long userId, @Param("courseId") Long courseId, @Param("role") Role role);

    @Modifying
    @Query(value = """
            insert ignore into role_user_assignment (user_id, role_id, permission_scope_id)
            select :userId, r.id, ps.id
            from role r, permission_scope ps
            where r.name = :roleName
              and ps.kind = :kind
              and coalesce(ps.course_id, 0) = coalesce(:courseId, 0)
              and coalesce(ps.education_resource_id, 0) = coalesce(:eduResId, 0)
            """, nativeQuery = true)
    int createIfAbsent(
            @Param("userId") long userId,
            @Param("roleName") String roleName,
            @Param("kind") String kind,
            @Param("courseId") Long courseId,
            @Param("eduResId") Long eduResId
    );

    @Modifying
    @Query("""
            delete from RoleUserAssignmentEntity rua
            where rua.user.id = :userId
              and rua.role.id = (select r.id from RoleEntity r where r.name = :role)
              and rua.permissionScope.id in (
                  select ps.id from PermissionScopeEntity ps
                  where ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.GLOBAL
                    and ps.course is null and ps.educationResource is null)
            """)
    int deleteGlobalRole(@Param("userId") long userId, @Param("role") Role role);

    @Modifying
    @Query("""
            delete from RoleUserAssignmentEntity rua
            where rua.user.id = :userId
              and rua.role.id = (select r.id from RoleEntity r where r.name = :role)
              and rua.permissionScope.id in (
                  select ps.id from PermissionScopeEntity ps
                  where ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.COURSE
                    and ps.course.id = :courseId)
            """)
    int deleteCourseRole(@Param("userId") long userId, @Param("role") Role role, @Param("courseId") Long courseId);

    @Modifying
    @Query("""
            delete from RoleUserAssignmentEntity rua
            where rua.user.id = :userId
              and rua.role.id = (select r.id from RoleEntity r where r.name = :role)
              and rua.permissionScope.id in (
                  select ps.id from PermissionScopeEntity ps
                  where ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.EDUCATION_RESOURCE
                    and ps.educationResource.id = :eduResId)
            """)
    int deleteEducationResourceRole(@Param("userId") long userId, @Param("role") Role role, @Param("eduResId") Long eduResId);

    @Modifying
    @Query("""
            delete from RoleUserAssignmentEntity rua
            where rua.user.id = :userId
              and rua.role.id not in (select r.id from RoleEntity r where r.name = :keepRole)
              and rua.permissionScope.id in (
                  select ps.id from PermissionScopeEntity ps
                  where ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.GLOBAL
                    and ps.course is null and ps.educationResource is null)
            """)
    int deleteGlobalRolesExcept(@Param("userId") long userId, @Param("keepRole") Role keepRole);

    @Modifying
    @Query("""
            delete from RoleUserAssignmentEntity rua
            where rua.user.id = :userId
              and rua.role.id not in (select r.id from RoleEntity r where r.name = :keepRole)
              and rua.permissionScope.id in (
                  select ps.id from PermissionScopeEntity ps
                  where ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.EDUCATION_RESOURCE
                    and ps.educationResource.id = :eduResId)
            """)
    int deleteEducationResourceRolesExcept(@Param("userId") long userId, @Param("eduResId") Long eduResId, @Param("keepRole") Role keepRole);


    @Query("""
            select rua from RoleUserAssignmentEntity rua
            join fetch rua.user u
            join fetch rua.role r
            join fetch rua.permissionScope ps
            join fetch ps.course c
            where u.id in :userIds
              and ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.COURSE
              and c.educationResource.id = :eduResId
            """)
    List<RoleUserAssignmentEntity> findCourseAssignmentsInEducationResource(
            @Param("eduResId") Long eduResId,
            @Param("userIds") Collection<Long> userIds
    );
}
