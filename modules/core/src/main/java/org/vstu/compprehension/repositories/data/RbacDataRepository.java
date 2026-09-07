package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.businesslogic.auth.Permission;
import org.vstu.compprehension.businesslogic.auth.Role;
import org.vstu.compprehension.data.cource.CourseRoleAssignmentData;
import org.vstu.compprehension.data.cource.CourseRoleGrantData;
import org.vstu.compprehension.businesslogic.auth.PermissionScopeKind;
import org.vstu.compprehension.entities.role.PermissionScopeEntity;
import org.vstu.compprehension.entities.role.RoleEntity;
import org.vstu.compprehension.entities.role.RoleUserAssignmentEntity;
import org.vstu.compprehension.repositories.entity.PermissionScopeRepository;
import org.vstu.compprehension.repositories.entity.RbacBulkInsertExecutor;
import org.vstu.compprehension.repositories.entity.RoleRepository;
import org.vstu.compprehension.repositories.entity.RoleUserAssignmentRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Права: кому какая роль выдана и в какой области.
 * <p>
 * Роли, области и выдачи — одна таблица связей и два справочника, которые поодиночке
 * бессмысленны: выдать роль значит завести область, если её ещё нет, и только потом
 * вставить связь. Разложенное по трём репозиториям, это заставляло сервис знать порядок
 * действий, который на самом деле является свойством схемы.
 * <p>
 * Проверки прав отвечают {@code boolean} и списками идентификаторов: сущности здесь
 * не при чём, наружу идут ответы на вопрос «можно ли», а не строки таблиц.
 */
@Repository
@RequiredArgsConstructor
public class RbacDataRepository {

    private final RoleUserAssignmentRepository ruaRepository;
    private final RoleRepository roleRepository;
    private final PermissionScopeRepository scopeRepository;
    private final RbacBulkInsertExecutor bulkInsertExecutor;

    // ---------------------------------------------------------------- проверки

    /** Есть ли у пользователя это право хотя бы в одной из областей. */
    @Transactional(readOnly = true)
    public boolean isAuthorizedInAnyScope(long userId, @NotNull String permissionId,
                                          @NotNull Collection<String> scopeKeys) {
        return ruaRepository.isAuthorizedInAnyScope(userId, permissionId, scopeKeys) != 0L;
    }

    /** Все права пользователя, действующие хотя бы в одной из областей. */
    @Transactional(readOnly = true)
    public @NotNull List<String> findPermissionIdsInAnyScope(long userId,
                                                             @NotNull Collection<String> scopeKeys) {
        return ruaRepository.findPermissionIdsInAnyScope(userId, scopeKeys);
    }

    /** Выдана ли пользователю именно эта роль именно в этой области. */
    @Transactional(readOnly = true)
    public boolean hasRoleInScope(long userId, @NotNull Role role, @NotNull PermissionScopeKind kind,
                                  @Nullable Long scopeItemId) {
        return ruaRepository.existsRoleInScope(userId, role, kind, scopeItemId);
    }

    /** Области заданного вида, в которых у пользователя есть это право. */
    @Transactional(readOnly = true)
    public @NotNull List<Long> findScopeItemIdsWithPermission(long userId, @NotNull Permission permission,
                                                              @NotNull PermissionScopeKind kind) {
        return ruaRepository.findScopeItemIdsWithPermission(userId, permission, kind);
    }

    // ---------------------------------------------------------------- выдача

    /**
     * Выдать роль в области, заведя саму область, если её ещё нет.
     * <p>
     * Повторная выдача той же роли ничего не меняет.
     */
    @Transactional
    public void grantRole(long userId, @NotNull Role role, @NotNull PermissionScopeKind kind,
                          @Nullable Long scopeItemId) {
        scopeRepository.createIfAbsent(kind.name(), scopeItemId);
        ruaRepository.createIfAbsent(userId, role.id(), kind.name(), scopeItemId);
    }

    /**
     * Снять в области все роли пользователя, кроме одной.
     *
     * @param keepRole роль, которую надо оставить; null — снять все
     */
    @Transactional
    public void revokeRolesInScopeExcept(long userId, @Nullable Role keepRole,
                                         @NotNull PermissionScopeKind kind, @Nullable Long scopeItemId) {
        ruaRepository.deleteRolesInScopeExcept(userId, keepRole, kind, scopeItemId);
    }

    /** Роли перечисленных пользователей во всех курсах образовательного ресурса. */
    @Transactional(readOnly = true)
    public @NotNull List<CourseRoleAssignmentData> findCourseRoleAssignments(
            long educationResourceId, @NotNull Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return ruaRepository.findCourseAssignmentsInEducationResource(educationResourceId, userIds).stream()
                .map(RbacDataRepository::toData)
                .toList();
    }

    /**
     * Применить рассчитанные изменения ролей в курсах: сначала выдачи, потом снятия.
     * <p>
     * Одним вызовом, потому что смена роли — это снятие плюс выдача, и между ними
     * пользователь не должен оказаться ни без роли, ни с двумя. Области курсов
     * заводятся здесь же: без них вставлять связь некуда.
     *
     * @throws IllegalStateException если роли нет в справочнике или область курса
     *                               не удалось завести
     */
    @Transactional
    public void applyCourseRoleChanges(@NotNull Collection<CourseRoleGrantData> grants,
                                       @NotNull Collection<Long> assignmentIdsToRevoke) {
        if (grants.isEmpty() && assignmentIdsToRevoke.isEmpty()) {
            return;
        }

        if (!grants.isEmpty()) {
            var roleEntities = findRoles(grants.stream().map(CourseRoleGrantData::role).collect(Collectors.toSet()));
            var courseScopes = createCourseScopesIfAbsent(
                    grants.stream().map(CourseRoleGrantData::courseId).collect(Collectors.toSet()));

            bulkInsertExecutor.insertRoleAssignmentsIgnoringDuplicates(grants.stream()
                    .map(grant -> new RbacBulkInsertExecutor.RoleAssignmentRow(
                            grant.userId(),
                            require(roleEntities, grant.role(), "Role missing in DB: ").getId(),
                            require(courseScopes, grant.courseId(), "Course scope missing for courseId=").getId()))
                    .toList());
        }

        if (!assignmentIdsToRevoke.isEmpty()) {
            ruaRepository.deleteAllById(assignmentIdsToRevoke);
        }
    }

    // ---------------------------------------------------------------- внутреннее

    private @NotNull Map<Role, RoleEntity> findRoles(@NotNull Set<Role> roles) {
        return roleRepository.findByNameIn(roles).stream()
                .collect(Collectors.toMap(RoleEntity::getName, role -> role));
    }

    /** Области курсов; недостающие заводятся и перечитываются одним запросом. */
    private @NotNull Map<Long, PermissionScopeEntity> createCourseScopesIfAbsent(@NotNull Set<Long> courseIds) {
        var byCourseId = new HashMap<Long, PermissionScopeEntity>();
        scopeRepository.findByKindAndScopeItemIdIn(PermissionScopeKind.COURSE, courseIds)
                .forEach(scope -> byCourseId.put(scope.getScopeItemId(), scope));

        var missing = courseIds.stream().filter(id -> !byCourseId.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            bulkInsertExecutor.insertPermissionScopesIgnoringDuplicates(missing.stream()
                    .map(id -> new RbacBulkInsertExecutor.PermissionScopeRow(
                            PermissionScopeKind.COURSE.name(), id))
                    .toList());
            scopeRepository.findByKindAndScopeItemIdIn(PermissionScopeKind.COURSE, missing)
                    .forEach(scope -> byCourseId.put(scope.getScopeItemId(), scope));
        }
        return byCourseId;
    }

    private static <K, V> @NotNull V require(@NotNull Map<K, V> map, @NotNull K key,
                                             @NotNull String messagePrefix) {
        var value = map.get(key);
        if (value == null) {
            throw new IllegalStateException(messagePrefix + key);
        }
        return value;
    }

    // ---------------------------------------------------------------- маппинг

    private static @NotNull CourseRoleAssignmentData toData(@NotNull RoleUserAssignmentEntity entity) {
        long id = Strict.required(entity.getId(), "id", "role assignment");
        String owner = "role assignment " + id;
        // Все три связи подняты join fetch'ем того же запроса.
        var user = Strict.required(entity.getUser(), "user", owner);
        var role = Strict.required(entity.getRole(), "role", owner);
        var scope = Strict.required(entity.getPermissionScope(), "permissionScope", owner);
        return new CourseRoleAssignmentData(
                id,
                Strict.required(user.getId(), "user.id", owner),
                // Область вида COURSE всегда указывает на курс: выборка ограничена ими.
                Strict.required(scope.getScopeItemId(), "permissionScope.scopeItemId", owner),
                Strict.required(role.getName(), "role.name", owner));
    }
}
