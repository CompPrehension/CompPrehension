package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.businesslogic.auth.AuthScope;
import org.vstu.compprehension.businesslogic.auth.Permission;
import org.vstu.compprehension.businesslogic.auth.PermissionList;
import org.vstu.compprehension.businesslogic.auth.Role;
import org.vstu.compprehension.businesslogic.auth.PermissionScope;
import org.vstu.compprehension.businesslogic.auth.PermissionScopeKind;
import org.vstu.compprehension.repositories.data.RbacDataRepository;

import java.util.List;

/**
 * Проверки прав. Выдача ролей - в {@link RoleAssignmentService}.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RbacDataRepository rbac;

    public boolean isAuthorized(long userId, Permission permission, AuthScope scope) {
        AuthScope applicable = scope.allowing(permission);
        if (applicable.isEmpty()) {
            return false;
        }
        return rbac.isAuthorizedInAnyScope(userId, permission.id(), applicable.queryKeys());
    }

    public void ensureAuthorized(long userId, Permission permission, AuthScope scope) {
        if (!isAuthorized(userId, permission, scope)) {
            throw new SecurityException(String.format(
                    "User %s has no %s permission in %s", userId, permission.id(), scope.queryKeys()));
        }
    }

    public PermissionList getPermissions(long userId, AuthScope scope) {
        if (scope.isEmpty()) {
            return PermissionList.none();
        }
        return PermissionList.of(rbac.findPermissionIdsInAnyScope(userId, scope.queryKeys()));
    }

    public boolean hasRole(long userId, Role role, PermissionScope scope) {
        return rbac.hasRoleInScope(userId, role, scope.kind(), scope.itemId());
    }

    public List<Long> findScopeItemIdsWithPermission(long userId, Permission permission, PermissionScopeKind kind) {
        return rbac.findScopeItemIdsWithPermission(userId, permission, kind);
    }
}
