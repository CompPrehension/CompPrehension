package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.businesslogic.auth.Role;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.role.PermissionScopeEntity;
import org.vstu.compprehension.models.entities.role.RoleEntity;
import org.vstu.compprehension.models.entities.role.RoleUserAssignmentEntity;
import org.vstu.compprehension.models.repository.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис выдачи и синхронизации ролей.
 */
@Service
@RequiredArgsConstructor
public class RoleAssignmentService {

    public record CourseRoleAssignment(Long userId, Long courseId, Role role) {
    }

    private final RoleUserAssignmentRepository ruaRepository;
    private final RoleRepository roleRepository;
    private final PermissionScopeRepository scopeRepository;
    private final RbacBulkInsertExecutor rbacBulkInsertExecutor;

    @Transactional
    public void assignGlobalRole(long userId, Role role) {
        assignRoleInternal(userId, role, PermissionScopeKind.GLOBAL, null);
    }

    @Transactional
    public void reconcileRoleInEducationResource(long userId, Long educationResourceId, Role desiredRole) {
        ruaRepository.deleteRolesInScopeExcept(userId, desiredRole, PermissionScopeKind.EDUCATION_RESOURCE, educationResourceId);
        if (desiredRole != null) {
            assignRoleInternal(userId, desiredRole, PermissionScopeKind.EDUCATION_RESOURCE, educationResourceId);
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

        List<RoleUserAssignmentEntity> existing =
                ruaRepository.findCourseAssignmentsInEducationResource(educationResourceId, userIdsToReconcile);

        Map<Long, Map<Long, RoleUserAssignmentEntity>> currentByUserAndCourse = new HashMap<>();
        for (RoleUserAssignmentEntity rua : existing) {
            currentByUserAndCourse
                    .computeIfAbsent(rua.getUser().getId(), nothing -> new HashMap<>())
                    .put(rua.getPermissionScope().getScopeItemId(), rua);
        }

        List<CourseRoleAssignment> courseInserts = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        Map<Long, Map<Long, Role>> desiredByUserAndCourse = new HashMap<>();

        for (CourseRoleAssignment desired : desiredAssignments) {
            if (desired.role() != null) {
                // Пакетная вставка минует assignRoleInternal, поэтому область проверяем здесь.
                ensureRoleAllowedIn(desired.role(), PermissionScopeKind.COURSE);
            }
            desiredByUserAndCourse
                    .computeIfAbsent(desired.userId(), nothing -> new HashMap<>())
                    .put(desired.courseId(), desired.role());

            RoleUserAssignmentEntity current = currentByUserAndCourse
                    .getOrDefault(desired.userId(), Map.of())
                    .get(desired.courseId());

            if (current == null) {
                if (desired.role() != null) {
                    courseInserts.add(desired);
                }
                continue;
            }
            if (java.util.Objects.equals(current.getRole().getName(), desired.role())) {
                continue;
            }
            toDelete.add(current.getId());
            if (desired.role() != null) {
                courseInserts.add(desired);
            }
        }

        Set<Long> sweepable = new HashSet<>(coursesToSweep);
        for (Map.Entry<Long, Map<Long, RoleUserAssignmentEntity>> userEntry : currentByUserAndCourse.entrySet()) {
            Map<Long, Role> desiredForUser = desiredByUserAndCourse.getOrDefault(userEntry.getKey(), Map.of());
            for (Map.Entry<Long, RoleUserAssignmentEntity> courseEntry : userEntry.getValue().entrySet()) {
                if (sweepable.contains(courseEntry.getKey()) && !desiredForUser.containsKey(courseEntry.getKey())) {
                    toDelete.add(courseEntry.getValue().getId());
                }
            }
        }

        if (courseInserts.isEmpty() && toDelete.isEmpty()) {
            return;
        }

        Map<Role, RoleEntity> roleById;
        if (courseInserts.isEmpty()) {
            roleById = Map.of();
        } else {
            Set<Role> neededRoles = courseInserts.stream()
                    .map(CourseRoleAssignment::role)
                    .collect(Collectors.toSet());
            roleById = roleRepository.findByNameIn(neededRoles).stream()
                    .collect(Collectors.toMap(RoleEntity::getName, Function.identity()));
        }

        Map<Long, PermissionScopeEntity> courseScopeByCourseId = resolveCourseScopes(courseInserts);

        rbacBulkInsertExecutor.insertRoleAssignmentsIgnoringDuplicates(
                courseInserts.stream()
                        .map(draft -> new RbacBulkInsertExecutor.RoleAssignmentRow(
                                draft.userId(),
                                requireRole(roleById, draft.role()).getId(),
                                requireCourseScope(courseScopeByCourseId, draft.courseId()).getId()))
                        .toList()
        );

        if (!toDelete.isEmpty()) {
            ruaRepository.deleteAllById(toDelete);
        }
    }

    private void assignRoleInternal(long userId, Role role, PermissionScopeKind kind, Long scopeItemId) {
        ensureRoleAllowedIn(role, kind);
        scopeRepository.createIfAbsent(kind.name(), scopeItemId);
        ruaRepository.createIfAbsent(userId, role.id(), kind.name(), scopeItemId);
    }

    private static void ensureRoleAllowedIn(Role role, PermissionScopeKind kind) {
        if (!role.isAllowedIn(kind)) {
            throw new IllegalArgumentException(String.format(
                    "Role %s cannot be assigned in scope %s", role.id(), kind));
        }
    }

    private Map<Long, PermissionScopeEntity> resolveCourseScopes(List<CourseRoleAssignment> drafts) {
        if (drafts.isEmpty()) {
            return Map.of();
        }
        Set<Long> courseIds = drafts.stream()
                .map(CourseRoleAssignment::courseId)
                .collect(Collectors.toSet());
        Map<Long, PermissionScopeEntity> courseIdToScope = new HashMap<>();
        scopeRepository.findByKindAndScopeItemIdIn(PermissionScopeKind.COURSE, courseIds)
                .forEach(ps -> courseIdToScope.put(ps.getScopeItemId(), ps));

        List<Long> coursesWithoutScope = courseIds.stream()
                .filter(cid -> !courseIdToScope.containsKey(cid))
                .toList();
        if (!coursesWithoutScope.isEmpty()) {
            rbacBulkInsertExecutor.insertPermissionScopesIgnoringDuplicates(
                    coursesWithoutScope.stream()
                            .map(cid -> new RbacBulkInsertExecutor.PermissionScopeRow(
                                    PermissionScopeKind.COURSE.name(), cid))
                            .toList());

            scopeRepository.findByKindAndScopeItemIdIn(PermissionScopeKind.COURSE, coursesWithoutScope)
                    .forEach(ps -> courseIdToScope.put(ps.getScopeItemId(), ps));
        }
        return courseIdToScope;
    }

    private static RoleEntity requireRole(Map<Role, RoleEntity> map, Role role) {
        var entity = map.get(role);
        if (entity == null) {
            throw new IllegalStateException(String.format("Role missing in DB: %s", role));
        }
        return entity;
    }

    private static PermissionScopeEntity requireCourseScope(Map<Long, PermissionScopeEntity> map, Long courseId) {
        var scope = map.get(courseId);
        if (scope == null) {
            throw new IllegalStateException(String.format("Course scope missing for courseId=%s", courseId));
        }
        return scope;
    }
}
