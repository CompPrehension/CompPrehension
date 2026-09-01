package org.vstu.compprehension.models.businesslogic.auth;

import java.util.Collection;
import java.util.Set;

/**
 * Права, которыми пользователь обладает в некотором наборе областей.
 */
public final class PermissionList {

    private static final PermissionList NONE = new PermissionList(Set.of());

    private final Set<String> ids;

    private PermissionList(Set<String> ids) {
        this.ids = ids;
    }

    public static PermissionList of(Collection<String> permissionIds) {
        return permissionIds.isEmpty() ? NONE : new PermissionList(Set.copyOf(permissionIds));
    }

    public static PermissionList none() {
        return NONE;
    }

    public boolean contains(Permission permission) {
        return ids.contains(permission.id());
    }

    public Set<String> ids() {
        return ids;
    }
}
