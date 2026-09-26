package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.businesslogic.auth.AuthScope;
import org.vstu.compprehension.businesslogic.auth.Capability;
import org.vstu.compprehension.businesslogic.auth.Permission;
import org.vstu.compprehension.businesslogic.auth.PermissionList;
import org.vstu.compprehension.businesslogic.auth.Role;
import org.vstu.compprehension.businesslogic.auth.PermissionScope;
import org.vstu.compprehension.businesslogic.auth.PermissionScopeKind;
import org.vstu.compprehension.repositories.data.RbacDataRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
class AuthServiceImpl implements AuthService {

    private final RbacDataRepository rbac;

    public boolean isAuthorized(long userId, @NonNull Permission permission, @NonNull AuthScope scope) {
        if (scope.isEmpty()) {
            return false;
        }
        return rbac.isAuthorizedInAnyScope(userId, permission.id(), scope.queryKeys());
    }

    public void ensureAuthorized(long userId, @NonNull Permission permission, @NonNull AuthScope scope) {
        if (!isAuthorized(userId, permission, scope)) {
            throw new SecurityException(String.format(
                    "User %s has no %s permission in %s", userId, permission.id(), scope.queryKeys()));
        }
    }

    public boolean isAuthorized(long userId, @NonNull Capability capability) {
        return rbac.isAuthorizedIgnoringScope(userId, capability.id());
    }

    public void ensureAuthorized(long userId, @NonNull Capability capability) {
        if (!isAuthorized(userId, capability)) {
            throw new SecurityException(String.format(
                    "User %s has no %s capability", userId, capability.id()));
        }
    }

    public PermissionList getPermissions(long userId, @NonNull AuthScope scope) {
        if (scope.isEmpty()) {
            return PermissionList.none();
        }
        return PermissionList.of(rbac.findPermissionIdsInAnyScope(userId, scope.queryKeys()));
    }

    public boolean hasRole(long userId, @NonNull Role role, @NonNull PermissionScope scope) {
        return rbac.hasRoleInScope(userId, role, scope.kind(), scope.itemId());
    }

    public List<Long> findScopeItemIdsWithPermission(long userId, @NonNull Permission permission, @NonNull PermissionScopeKind kind) {
        return rbac.findScopeItemIdsWithPermission(userId, permission, kind);
    }
}
