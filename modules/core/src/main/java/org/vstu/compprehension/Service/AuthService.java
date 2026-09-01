package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.businesslogic.auth.PermissionList;
import org.vstu.compprehension.models.businesslogic.auth.AuthScope;
import org.vstu.compprehension.models.businesslogic.auth.Permission;
import org.vstu.compprehension.models.businesslogic.auth.Role;
import org.vstu.compprehension.models.entities.EnumData.PermissionScope;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.repository.*;

import java.util.List;

/**
 * Проверки прав. Выдача ролей - в {@link RoleAssignmentService}.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RoleUserAssignmentRepository ruaRepository;

    public boolean isAuthorized(long userId, Permission permission, AuthScope scope) {
        AuthScope applicable = scope.allowing(permission);
        if (applicable.isEmpty()) {
            return false;
        }
        return ruaRepository.isAuthorizedInAnyScope(userId, permission.id(), applicable.queryKeys()) != 0L;
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
        return PermissionList.of(ruaRepository.findPermissionIdsInAnyScope(userId, scope.queryKeys()));
    }

    public boolean hasRole(long userId, Role role, PermissionScope scope) {
        return ruaRepository.existsRoleInScope(userId, role, scope.kind(), scope.itemId());
    }

    public List<Long> findScopeItemIdsWithPermission(long userId, Permission permission, PermissionScopeKind kind) {
        return ruaRepository.findScopeItemIdsWithPermission(userId, permission, kind);
    }
}
