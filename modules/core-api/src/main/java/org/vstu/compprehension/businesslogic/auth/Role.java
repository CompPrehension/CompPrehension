package org.vstu.compprehension.businesslogic.auth;

import java.util.Set;

/**
 * Роль.
 */
public interface Role {

    String id();

    Set<Permission> getPermissions();

    Set<Capability> getCapabilities();

    boolean isAllowedIn(PermissionScopeKind kind);
}
