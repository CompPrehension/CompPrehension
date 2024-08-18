package org.vstu.compprehension.models.businesslogic.auth;

import org.jetbrains.annotations.Nullable;

/** Область действия права */
public record PermissionScope(PermissionScopeKind Scope, @Nullable Long ObjectId) {
    public PermissionScope(PermissionScopeKind scope) {
        this(scope, null);
        if (scope != PermissionScopeKind.GLOBAL) {
            throw new RuntimeException("Only GLOBAL scope is supported for usage without object id");
        }
    }
};
