package org.vstu.compprehension.businesslogic.auth;

/**
 * Право.
 */
public interface Permission {

    /** Значение колонки {@code permission.name}. */
    String id();

    boolean isAllowedIn(PermissionScopeKind kind);
}
