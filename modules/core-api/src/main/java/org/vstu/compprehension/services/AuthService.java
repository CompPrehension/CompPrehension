package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.auth.*;

import java.util.List;

public interface AuthService {
    boolean isAuthorized(long userId, @NotNull Permission permission, @NotNull AuthScope scope);

    void ensureAuthorized(long userId, @NotNull Permission permission, @NotNull AuthScope scope);

    PermissionList getPermissions(long userId,  @NotNull AuthScope scope);

    boolean hasRole(long userId, Role role, PermissionScope scope);

    List<Long> findScopeItemIdsWithPermission(long userId, Permission permission, PermissionScopeKind kind);
}
