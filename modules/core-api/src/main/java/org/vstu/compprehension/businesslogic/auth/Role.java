package org.vstu.compprehension.businesslogic.auth;

import java.util.Set;

/**
 * Роль.
 */
public interface Role {

    String id();

    Set<Permission> getPermissions();

    boolean isAllowedIn(PermissionScopeKind kind);
}
