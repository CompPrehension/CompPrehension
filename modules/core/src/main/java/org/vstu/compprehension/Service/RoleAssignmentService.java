package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.businesslogic.auth.Role;
import org.vstu.compprehension.models.data.CourseRoleAssignmentData;
import org.vstu.compprehension.models.data.CourseRoleGrantData;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.repository.data.RbacDataRepository;

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
@RequiredArgsConstructor
public class RoleAssignmentService {

    /**
     * Роль, которая должна быть у пользователя в курсе.
     *
     * @param role null — роли в этом курсе быть не должно
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
    public void reconcileRoleInEducationResource(long userId, Long educationResourceId,
                                                 @Nullable Role desiredRole) {
        rbac.revokeRolesInScopeExcept(
                userId, desiredRole, PermissionScopeKind.EDUCATION_RESOURCE, educationResourceId);
        if (desiredRole != null) {
            ensureRoleAllowedIn(desiredRole, PermissionScopeKind.EDUCATION_RESOURCE);
            rbac.grantRole(userId, desiredRole, PermissionScopeKind.EDUCATION_RESOURCE, educationResourceId);
        }
    }

    /**
     * Привести роли пользователей в курсах образовательного ресурса к желаемым.
     *
     * @param userIdsToReconcile пользователи, чьи роли пересматриваются; для остальных
     *                           ничего не меняется
     * @param desiredAssignments желаемое состояние; роль {@code null} означает, что роли
     *                           в этом курсе быть не должно
     * @param coursesToSweep     курсы, в которых роль, не упомянутая в желаемом состоянии,
     *                           снимается; в остальных курсах лишние роли остаются
     */
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

        // Роль в подметаемом курсе, о которой желаемое состояние молчит, снимается:
        // именно так уходит роль пользователя, отчисленного из курса во внешней системе.
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

    private static void ensureRoleAllowedIn(@NotNull Role role, @NotNull PermissionScopeKind kind) {
        if (!role.isAllowedIn(kind)) {
            throw new IllegalArgumentException(String.format(
                    "Role %s cannot be assigned in scope %s", role.id(), kind));
        }
    }
}
