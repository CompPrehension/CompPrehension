package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.entities.EnumData.Permission;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.EnumData.Role;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    public record CourseRoleAssignment(Long userId, Long courseId, Role role) {
    }

    private final RoleUserAssignmentRepository ruaRepository;
    private final RoleRepository roleRepository;
    private final PermissionScopeRepository scopeRepository;
    private final RbacBulkInsertExecutor rbacBulkInsertExecutor;

    public boolean isAuthorizedGlobal(long userId, Permission permission) {
        return ruaRepository.isUserAuthorized(userId, permission, null, null);
    }

    public boolean isGlobalAdmin(long userId) {
        return ruaRepository.existsByUser_IdAndRole_NameAndPermissionScope_Kind(
                userId, Role.GLOBAL_ADMIN, PermissionScopeKind.GLOBAL);
    }

    public boolean isAuthorizedInCourse(long userId, Permission permission, Long courseId) {
        if (courseId == null) {
            return false;
        }
        return ruaRepository.isUserAuthorized(userId, permission, courseId, null);
    }

    public boolean isAuthorizedInEducationResource(long userId, Permission permission, Long educationResourceId) {
        if (educationResourceId == null) {
            return false;
        }
        return ruaRepository.isUserAuthorized(userId, permission, null, educationResourceId);
    }

    public void ensureAuthorizedGlobal(long userId, Permission permission) {
        if (!isAuthorizedGlobal(userId, permission)) {
            throw new SecurityException(String.format("User %s has no %s permission (global)", userId, permission));
        }
    }

    public void ensureAuthorizedInCourse(long userId, Permission permission, Long courseId) {
        if (!isAuthorizedInCourse(userId, permission, courseId)) {
            throw new SecurityException(String.format("User %s has no %s permission in course %s", userId, permission, courseId));
        }
    }

    public void ensureAuthorizedInEducationResource(long userId, Permission permission, Long educationResourceId) {
        if (!isAuthorizedInEducationResource(userId, permission, educationResourceId)) {
            throw new SecurityException(String.format("User %s has no %s permission in educationResource %s", userId, permission, educationResourceId));
        }
    }

    public void ensureAuthorized(long userId, Permission permission, Long courseId) {
        if (courseId != null) {
            ensureAuthorizedInCourse(userId, permission, courseId);
        } else {
            ensureAuthorizedGlobal(userId, permission);
        }
    }

    @Transactional
    public void assignGlobalRole(long userId, Role role) {
        assignRoleInternal(userId, role, PermissionScopeKind.GLOBAL, null, null);
    }

    @Transactional
    public void assignRoleInCourse(long userId, Role role, Long courseId) {
        assignRoleInternal(userId, role, PermissionScopeKind.COURSE, courseId, null);
    }

    @Transactional
    public void assignRoleInEducationResource(long userId, Role role, Long educationResourceId) {
        assignRoleInternal(userId, role, PermissionScopeKind.EDUCATION_RESOURCE, null, educationResourceId);
    }

    @Transactional
    public void revokeGlobalRole(long userId, Role role) {
        ruaRepository.deleteGlobalRole(userId, role);
    }

    @Transactional
    public void revokeRoleInCourse(long userId, Role role, Long courseId) {
        ruaRepository.deleteCourseRole(userId, role, courseId);
    }

    @Transactional
    public void revokeRoleInEducationResource(long userId, Role role, Long educationResourceId) {
        ruaRepository.deleteEducationResourceRole(userId, role, educationResourceId);
    }

    public List<Role> findRolesInCourse(long userId, Long courseId) {
        return ruaRepository.findRolesInScope(userId, courseId);
    }

    /**
     * Есть ли у пользователя указанная роль в рамках курса.
     *
     * @param userId   пользователь
     * @param courseId курс; {@code null} трактуется как "нет курса" и даёт {@code false}
     * @param role     искомая роль
     * @return {@code true}, если роль назначена пользователю в этом курсе
     */
    public boolean hasRoleInCourse(long userId, Long courseId, Role role) {
        return courseId != null && ruaRepository.existsRoleInScope(userId, courseId, role);
    }

    private void assignRoleInternal(long userId, Role role, PermissionScopeKind kind, Long courseId, Long educationResourceId) {
        scopeRepository.createIfAbsent(kind.name(), courseId, educationResourceId);
        ruaRepository.createIfAbsent(userId, role.name(), kind.name(), courseId, educationResourceId);
    }

    /**
     * Делает указанную роль единственной GLOBAL-ролью пользователя: прочие GLOBAL-роли снимаются,
     * заданная назначается (если её ещё нет). {@code desiredRole == null} -> снять все GLOBAL-роли.
     *
     * @param userId      пользователь
     * @param desiredRole единственная роль, которая должна остаться на GLOBAL scope; {@code null} — ни одной
     */
    @Transactional
    public void reconcileGlobalRole(long userId, Role desiredRole) {
        ruaRepository.deleteGlobalRolesExcept(userId, desiredRole);
        if (desiredRole != null) {
            assignRoleInternal(userId, desiredRole, PermissionScopeKind.GLOBAL, null, null);
        }
    }

    /**
     * Делает указанную роль единственной ролью пользователя в данном education-resource: прочие
     * роли на этом scope снимаются, заданная назначается (если её ещё нет).
     * {@code desiredRole == null} -> снять все роли пользователя в этом education-resource.
     *
     * @param userId              пользователь
     * @param educationResourceId education-resource, в котором сверяется роль
     * @param desiredRole         единственная роль, которая должна остаться; {@code null} — ни одной
     */
    @Transactional
    public void reconcileRoleInEducationResource(long userId, Long educationResourceId, Role desiredRole) {
        ruaRepository.deleteEducationResourceRolesExcept(userId, educationResourceId, desiredRole);
        if (desiredRole != null) {
            assignRoleInternal(userId, desiredRole, PermissionScopeKind.EDUCATION_RESOURCE, null, educationResourceId);
        }
    }

    /**
     * Приводит COURSE-роли указанных пользователей в соответствие с {@code desiredAssignments}.
     * Для каждой пары (пользователь, курс) из этого набора метод гарантирует, что после вызова
     * у пользователя в данном курсе будет именно заданная роль: если роли не было — она
     * назначается; если была другая — заменяется; если роль задана пустой ({@code role == null}) —
     * текущая снимается. Можно передать как одну пару, так и сразу несколько.
     *
     * <p>Роли уровня EDUCATION_RESOURCE_ADMIN метод не трогает и никогда не снимает.
     *
     * <p>Лишние роли (которых нет в {@code desiredAssignments}) снимаются ТОЛЬКО в курсах из
     * {@code coursesToSweep}; роли в остальных курсах сохраняются. Так вызывающий ограничивает
     * зону снятия, например LTI-вход передаёт сюда только текущий курс.
     *
     * @param educationResourceId education-resource, в рамках которого сверяются роли
     * @param userIdsToReconcile  пользователи, у которых проверяются и при необходимости снимаются COURSE-роли
     * @param desiredAssignments  желаемые роли по парам (user, course); {@code role == null} - роли быть не должно
     * @param coursesToSweep      курсы, в которых разрешено снимать роли, отсутствующие в {@code desiredAssignments}
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

        List<RoleUserAssignmentEntity> existing =
                ruaRepository.findCourseAssignmentsInEducationResource(educationResourceId, userIdsToReconcile);

        // userId -> (courseId -> текущее COURSE-назначение)
        Map<Long, Map<Long, RoleUserAssignmentEntity>> currentByUserAndCourse = new HashMap<>();
        for (RoleUserAssignmentEntity rua : existing) {
            currentByUserAndCourse
                    .computeIfAbsent(rua.getUser().getId(), nothing -> new HashMap<>())
                    .put(rua.getPermissionScope().getCourse().getId(), rua);
        }

        List<CourseRoleAssignment> courseInserts = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        // userId -> (courseId -> желаемая роль): нужно, чтобы при sweep отличить "нет в desired"
        Map<Long, Map<Long, Role>> desiredByUserAndCourse = new HashMap<>();

        for (CourseRoleAssignment desired : desiredAssignments) {
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
            if (current.getRole().getName() == desired.role()) {
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

        Map<Role, RoleEntity> roleByEnum;
        if (courseInserts.isEmpty()) {
            roleByEnum = Map.of();
        } else {
            Set<Role> neededRoles = courseInserts.stream()
                    .map(CourseRoleAssignment::role)
                    .collect(Collectors.toSet());
            roleByEnum = roleRepository.findByNameIn(neededRoles).stream()
                    .collect(Collectors.toMap(RoleEntity::getName, Function.identity()));
        }

        Map<Long, PermissionScopeEntity> courseScopeByCourseId = resolveCourseScopes(courseInserts);

        rbacBulkInsertExecutor.insertRoleAssignmentsIgnoringDuplicates(
                courseInserts.stream()
                        .map(draft -> new RbacBulkInsertExecutor.RoleAssignmentRow(
                                draft.userId(),
                                requireRole(roleByEnum, draft.role()).getId(),
                                requireCourseScope(courseScopeByCourseId, draft.courseId()).getId()))
                        .toList()
        );

        if (!toDelete.isEmpty()) {
            ruaRepository.deleteAllById(toDelete);
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
        scopeRepository.findByKindAndCourseIdIn(PermissionScopeKind.COURSE, courseIds)
                .forEach(ps -> courseIdToScope.put(ps.getCourse().getId(), ps));

        List<Long> coursesWithoutScope = courseIds.stream()
                .filter(cid -> !courseIdToScope.containsKey(cid))
                .toList();
        if (!coursesWithoutScope.isEmpty()) {
            rbacBulkInsertExecutor.insertPermissionScopesIgnoringDuplicates(
                    coursesWithoutScope.stream()
                            .map(cid -> new RbacBulkInsertExecutor.PermissionScopeRow(
                                    PermissionScopeKind.COURSE.name(), cid, null))
                            .toList());

            scopeRepository.findByKindAndCourseIdIn(PermissionScopeKind.COURSE, coursesWithoutScope)
                    .forEach(ps -> courseIdToScope.put(ps.getCourse().getId(), ps));
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
