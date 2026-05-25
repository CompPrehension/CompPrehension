package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.EnumData.Permission;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.EnumData.Role;
import org.vstu.compprehension.models.entities.role.PermissionScopeEntity;
import org.vstu.compprehension.models.entities.role.RoleEntity;
import org.vstu.compprehension.models.entities.role.RoleUserAssignmentEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    /**
     * Distinct role names the user effectively holds in the given context, mirroring the scope
     * logic of {@link #isUserAuthorized}: GLOBAL_ADMIN always; for a course — its COURSE-scope and
     * its education-resource-scope roles; with no course — GLOBAL-scope roles.
     */
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

    List<RoleUserAssignmentEntity> findByUser_Id(long userId);

    Optional<RoleUserAssignmentEntity> findByUser_IdAndRoleAndPermissionScope(
        long userId, RoleEntity role, PermissionScopeEntity scope);

    boolean existsByUser_IdAndRoleAndPermissionScope(
        long userId, RoleEntity role, PermissionScopeEntity scope);

    /**
     * Loads all COURSE-scope and EDUCATION_RESOURCE-scope assignments for given users
     * within one environment in a single query, with all associations eagerly fetched.
     * Used by sync jobs (e.g. {@code MoodleRoleSyncService}) for bulk diff against the
     * current DB state вЂ” see {@code AuthService.applyEnvironmentAssignmentsDiff}.
     */
    @Query("""
        select rua from RoleUserAssignmentEntity rua
        join fetch rua.user u
        join fetch rua.role r
        join fetch rua.permissionScope ps
        left join fetch ps.course c
        where u.id in :userIds
          and (
              (ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.COURSE
                  and c.educationResource.id = :environmentId)
              or
              (ps.kind = org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind.EDUCATION_RESOURCE
                  and ps.educationResource.id = :environmentId)
          )
        """)
    List<RoleUserAssignmentEntity> findAssignmentsInEnvironment(
        @Param("environmentId") Long environmentId,
        @Param("userIds") Collection<Long> userIds
    );
}
