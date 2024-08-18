package org.vstu.compprehension.models.businesslogic.auth;

import org.jetbrains.annotations.Nullable;

/** Назначение роли пользователю */
public record UserRoleAssignment(
    long UserId,
    PermissionScopeKind Scope,
    @Nullable Long ScopeObjectId) {

    public UserRoleAssignment(long userId, PermissionScopeKind scope) {
        this(userId, scope, null);
        if (scope != PermissionScopeKind.GLOBAL) {
            throw new RuntimeException("Only GLOBAL scope is supported for usage without object id");
        }
    }
};
