package org.vstu.compprehension.models.businesslogic.auth;

import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;

/**
 * Право.
 */
public interface Permission {

    /** Значение колонки {@code permission.name}. */
    String id();

    boolean isAllowedIn(PermissionScopeKind kind);
}
