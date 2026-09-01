package org.vstu.compprehension.models.businesslogic.auth;

import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;

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
