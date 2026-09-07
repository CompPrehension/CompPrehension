package org.vstu.compprehension.businesslogic.auth;

import java.util.Set;

/**
 * Роль.
 */
public interface Role {

    /** Значение колонки {@code role.name}. */
    String id();

    Set<Permission> getPermissions();

    boolean isAllowedIn(PermissionScopeKind kind);
}
