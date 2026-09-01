package org.vstu.compprehension.models.businesslogic.auth;

import org.vstu.compprehension.models.entities.EnumData.PermissionScope;

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

    /** Отбрасывает области, в которых право неприменимо. */
    public AuthScope allowing(Permission permission) {
        return new AuthScope(scopes.stream()
                .filter(scope -> permission.isAllowedIn(scope.kind()))
                .toList());
    }

    /** Ключи строк {@code permission_scope}. */
    public List<String> queryKeys() {
        return scopes.stream().map(PermissionScope::queryKey).toList();
    }
}
