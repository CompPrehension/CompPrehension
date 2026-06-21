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
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.PermissionScopeRepository;
import org.vstu.compprehension.models.repository.RoleRepository;
import org.vstu.compprehension.models.repository.RoleUserAssignmentRepository;
import org.vstu.compprehension.models.repository.UserRepository;

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

    public record UserCourseKey(Long userId, Long courseId) {}

    private final RoleUserAssignmentRepository ruaRepository;
    private final RoleRepository roleRepository;
    private final PermissionScopeRepository scopeRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public boolean isAuthorizedGlobal(long userId, Permission permission) {
        return ruaRepository.isUserAuthorized(userId, permission, null, null);
    }

    /**
     * True only for users holding the GLOBAL_ADMIN role at GLOBAL scope. Use this (not a
     * permission check) to gate cross-tenant "see everything" actions: a non-admin global
     * role also carries permissions like VIEW_COURSE, so a permission check would over-grant.
     */
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

    private void assignRoleInternal(long userId, Role role, PermissionScopeKind kind, Long courseId, Long educationResourceId) {
        scopeRepository.createIfAbsent(kind.name(), courseId, educationResourceId);
        ruaRepository.createIfAbsent(userId, role.name(), kind.name(), courseId, educationResourceId);
    }

    /**
     * Bulk-reconciles COURSE-scope role assignments for one education-resource environment.
     * Single SELECT loads current state for all given users; a single batched INSERT/DELETE
     * applies the diff. Intended for sync jobs — avoids the per-pair N+1 of individual
     * {@link #assignRoleInCourse} calls.
     *
     * <p>EDUCATION_RESOURCE_ADMIN assignments are intentionally NOT touched: site-admin
     * status in Moodle is not exposed via WS in a way we can sync (site admins bypass
     * the capability system entirely), so admin assignments are managed via LTI launch
     * or direct SQL — sync must not delete them.
     *
     * <p>Снятие роли (sweep существующих назначений, отсутствующих в desired) выполняется
     * ТОЛЬКО для курсов из {@code managedCourseIds} — курсов, реально синхронизированных в
     * этом прогоне. Назначения по курсам вне этого набора (например, отвязанным от Moodle
     * после удаления курса) не трогаются — sync перестаёт ими управлять, роли сохраняются.
     *
     * @param environmentId          target EducationResource id
     * @param userIds                users in scope of this sync (typically: all with ExternalAccount for this env)
     * @param desiredCourseRoles     desired COURSE-scope role per (user, course); value {@code null} means "no role"
     * @param managedCourseIds       course ids синхронизированные в этом прогоне; только для них допускается снятие роли
     */
    @Transactional
    public void applyEnvironmentAssignmentsDiff(
            Long environmentId,
            Collection<Long> userIds,
            Map<UserCourseKey, Role> desiredCourseRoles,
            Collection<Long> managedCourseIds
    ) {
        if (userIds.isEmpty()) {
            return;
        }

        List<RoleUserAssignmentEntity> existing =
                ruaRepository.findAssignmentsInEducationResource(environmentId, userIds);

        Map<UserCourseKey, RoleUserAssignmentEntity> existingCourse = new HashMap<>();
        for (RoleUserAssignmentEntity rua : existing) {
            PermissionScopeEntity ps = rua.getPermissionScope();
            if (ps.getKind() == PermissionScopeKind.COURSE) {
                existingCourse.put(
                        new UserCourseKey(rua.getUser().getId(), ps.getCourse().getId()),
                        rua);
            }
        }

        List<CourseAssignmentDraft> courseInserts = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();

        for (Map.Entry<UserCourseKey, Role> entry : desiredCourseRoles.entrySet()) {
            Long userId = entry.getKey().userId();
            Long courseId = entry.getKey().courseId();
            Role desired = entry.getValue();
            RoleUserAssignmentEntity current = existingCourse.get(entry.getKey());

            if (current == null) {
                if (desired != null) {
                    courseInserts.add(new CourseAssignmentDraft(userId, courseId, desired));
                }
                continue;
            }
            Role currentRole = current.getRole().getName();
            if (currentRole == desired) {
                continue;
            }
            toDelete.add(current.getId());
            if (desired != null) {
                courseInserts.add(new CourseAssignmentDraft(userId, courseId, desired));
            }
        }
        Set<Long> managed = new HashSet<>(managedCourseIds);
        for (Map.Entry<UserCourseKey, RoleUserAssignmentEntity> e : existingCourse.entrySet()) {
            if (managed.contains(e.getKey().courseId()) && !desiredCourseRoles.containsKey(e.getKey())) {
                toDelete.add(e.getValue().getId());
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
                    .map(CourseAssignmentDraft::role)
                    .collect(Collectors.toSet());
            roleByEnum = roleRepository.findByNameIn(neededRoles).stream()
                    .collect(Collectors.toMap(RoleEntity::getName, Function.identity()));
        }

        Map<Long, PermissionScopeEntity> courseScopeByCourseId = resolveCourseScopes(courseInserts);

        List<RoleUserAssignmentEntity> toInsert = new ArrayList<>(courseInserts.size());
        for (CourseAssignmentDraft draft : courseInserts) {
            toInsert.add(new RoleUserAssignmentEntity(
                    userRepository.getReferenceById(draft.userId()),
                    requireRole(roleByEnum, draft.role()),
                    requireCourseScope(courseScopeByCourseId, draft.courseId())));
        }

        if (!toInsert.isEmpty()) {
            ruaRepository.saveAll(toInsert);
        }
        if (!toDelete.isEmpty()) {
            ruaRepository.deleteAllByIdInBatch(toDelete);
        }
    }

    private Map<Long, PermissionScopeEntity> resolveCourseScopes(List<CourseAssignmentDraft> drafts) {
        if (drafts.isEmpty()) {
            return Map.of();
        }
        Set<Long> courseIds = drafts.stream()
                .map(CourseAssignmentDraft::courseId)
                .collect(Collectors.toSet());

        Map<Long, PermissionScopeEntity> result = new HashMap<>();
        scopeRepository.findByKindAndCourseIdIn(PermissionScopeKind.COURSE, courseIds)
                .forEach(ps -> result.put(ps.getCourse().getId(), ps));

        List<PermissionScopeEntity> missing = courseIds.stream()
                .filter(cid -> !result.containsKey(cid))
                .map(cid -> PermissionScopeEntity.ofCourseScope(courseRepository.getReferenceById(cid)))
                .toList();
        if (!missing.isEmpty()) {
            scopeRepository.saveAll(missing)
                    .forEach(ps -> result.put(ps.getCourse().getId(), ps));
        }
        return result;
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

    private record CourseAssignmentDraft(Long userId, Long courseId, Role role) {}
}
