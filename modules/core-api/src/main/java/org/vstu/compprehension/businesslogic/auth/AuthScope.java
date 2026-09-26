package org.vstu.compprehension.businesslogic.auth;

import java.util.List;

/**
 * Скоуп авторизации, состоящий из одной или нескольких PermissionScope.
 */
public record AuthScope(List<PermissionScope> scopes) {

    public AuthScope {
        scopes = List.copyOf(scopes);
    }

    public static AuthScope of(PermissionScope... scopes) {
        return new AuthScope(List.of(scopes));
    }

    public boolean isEmpty() {
        return scopes.isEmpty();
    }

    /** Ключи строк {@code permission_scope}. */
    public List<String> queryKeys() {
        return scopes.stream().map(PermissionScope::queryKey).toList();
    }
}
