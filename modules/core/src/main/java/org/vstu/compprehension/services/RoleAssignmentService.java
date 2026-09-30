package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.businesslogic.auth.Role;
import org.vstu.compprehension.data.cource.CourseRoleAssignmentData;
import org.vstu.compprehension.data.cource.CourseRoleGrantData;
import org.vstu.compprehension.businesslogic.auth.PermissionScopeKind;
import org.vstu.compprehension.repositories.data.RbacDataRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Сервис выдачи и синхронизации ролей.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class RoleAssignmentService {

    /**
     * Роль, которая должна быть у пользователя в курсе.
     */
    public record CourseRoleAssignment(Long userId, Long courseId, @Nullable Role role) {
    }

    private final RbacDataRepository rbac;

    @Transactional
    public void assignGlobalRole(long userId, @NotNull Role role) {
        ensureRoleAllowedIn(role, PermissionScopeKind.GLOBAL);
        rbac.grantRole(userId, role, PermissionScopeKind.GLOBAL, null);
    }

    @Transactional
    public void assignRootRole(long userId, @NotNull Role role) {
        ensureRoleAllowedIn(role, PermissionScopeKind.ROOT);
        rbac.grantRole(userId, role, PermissionScopeKind.ROOT, null);
    }

    @Transactional
    public void reconcileRoleInEducationResource(long userId, Long educationResourceId,
                                                 @Nullable Role desiredRole) {
        rbac.revokeRolesInScopeExcept(
                userId, desiredRole, PermissionScopeKind.EDUCATION_RESOURCE, educationResourceId);
        if (desiredRole != null) {
            ensureRoleAllowedIn(desiredRole, PermissionScopeKind.EDUCATION_RESOURCE);
            rbac.grantRole(userId, desiredRole, PermissionScopeKind.EDUCATION_RESOURCE, educationResourceId);
        }
    }

    @Transactional
    public void reconcileCourseRoleAssignments(
            Long educationResourceId,
            Collection<Long> userIdsToReconcile,
            Collection<CourseRoleAssignment> desiredAssignments,
            Collection<Long> coursesToSweep
    ) {
        if (userIdsToReconcile.isEmpty()) {
            return;
        }

        Map<Long, Map<Long, CourseRoleAssignmentData>> currentByUserAndCourse = new HashMap<>();
        for (var current : rbac.findCourseRoleAssignments(educationResourceId, userIdsToReconcile)) {
            currentByUserAndCourse
                    .computeIfAbsent(current.userId(), nothing -> new HashMap<>())
                    .put(current.courseId(), current);
        }

        List<CourseRoleGrantData> grants = new ArrayList<>();
        List<Long> toRevoke = new ArrayList<>();
        Map<Long, Map<Long, Role>> desiredByUserAndCourse = new HashMap<>();

        for (CourseRoleAssignment desired : desiredAssignments) {
            if (desired.role() != null) {
                ensureRoleAllowedIn(desired.role(), PermissionScopeKind.COURSE);
            }
            desiredByUserAndCourse
                    .computeIfAbsent(desired.userId(), nothing -> new HashMap<>())
                    .put(desired.courseId(), desired.role());

            var current = currentByUserAndCourse
                    .getOrDefault(desired.userId(), Map.of())
                    .get(desired.courseId());

            if (current == null) {
                if (desired.role() != null) {
                    grants.add(new CourseRoleGrantData(desired.userId(), desired.courseId(), desired.role()));
                }
                continue;
            }
            if (Objects.equals(current.role(), desired.role())) {
                continue;
            }
            toRevoke.add(current.id());
            if (desired.role() != null) {
                grants.add(new CourseRoleGrantData(desired.userId(), desired.courseId(), desired.role()));
            }
        }

        Set<Long> sweepable = new HashSet<>(coursesToSweep);
        for (var userEntry : currentByUserAndCourse.entrySet()) {
            Map<Long, Role> desiredForUser = desiredByUserAndCourse.getOrDefault(userEntry.getKey(), Map.of());
            for (var courseEntry : userEntry.getValue().entrySet()) {
                if (sweepable.contains(courseEntry.getKey()) && !desiredForUser.containsKey(courseEntry.getKey())) {
                    toRevoke.add(courseEntry.getValue().id());
                }
            }
        }

        rbac.applyCourseRoleChanges(grants, toRevoke);
    }

    /**
     * Сверяет состав курса: из пользователей образовательного ресурса роль в курсе теряют те, кого нет
     * в {@code roleOfMember}, а участники без роли получают указанную. Имеющаяся роль участника не меняется.
     */
    @Transactional
    public void reconcileCourseMembers(long educationResourceId, long courseId,
                                       @NotNull Map<Long, Role> roleOfMember) {
        roleOfMember.values().forEach(role -> ensureRoleAllowedIn(role, PermissionScopeKind.COURSE));

        Set<Long> withRole = new HashSet<>();
        List<CourseRoleAssignmentData> toRevoke = new ArrayList<>();
        for (var current : rbac.findCourseRoleAssignmentsOfEducationResourceUsers(courseId, educationResourceId)) {
            if (roleOfMember.containsKey(current.userId())) {
                withRole.add(current.userId());
            } else {
                toRevoke.add(current);
            }
        }
        List<CourseRoleGrantData> grants = roleOfMember.entrySet().stream()
                .filter(member -> !withRole.contains(member.getKey()))
                .map(member -> new CourseRoleGrantData(member.getKey(), courseId, member.getValue()))
                .toList();

        rbac.applyCourseRoleChanges(grants, toRevoke.stream().map(CourseRoleAssignmentData::id).toList());
        grants.forEach(grant -> log.info("Course {}: user {} granted role {}", courseId, grant.userId(), grant.role().id()));
        toRevoke.forEach(revoked -> log.info("Course {}: user {} lost role {}", courseId, revoked.userId(), revoked.role().id()));
    }

    private static void ensureRoleAllowedIn(@NotNull Role role, @NotNull PermissionScopeKind kind) {
        if (!role.isAllowedIn(kind)) {
            throw new IllegalArgumentException(String.format(
                    "Role %s cannot be assigned in scope %s", role.id(), kind));
        }
    }
}
