package org.vstu.compprehension.models.entities.EnumData;

public record PermissionScope(PermissionScopeKind kind, Long itemId) {

    public PermissionScope {
        if (kind == null) {
            throw new IllegalArgumentException("Scope kind must not be null");
        }
        if (kind == PermissionScopeKind.GLOBAL) {
            if (itemId != null) {
                throw new IllegalArgumentException("GLOBAL scope must not carry an itemId");
            }
        } else if (itemId == null) {
            throw new IllegalArgumentException(kind + " scope requires a non-null itemId");
        }
    }

    public String queryKey() {
        return kind.name() + ":" + (itemId == null ? 0 : itemId);
    }

    public static PermissionScope global() {
        return new PermissionScope(PermissionScopeKind.GLOBAL, null);
    }

    public static PermissionScope course(Long courseId) {
        return new PermissionScope(PermissionScopeKind.COURSE, courseId);
    }

    public static PermissionScope educationResource(Long educationResourceId) {
        return new PermissionScope(PermissionScopeKind.EDUCATION_RESOURCE, educationResourceId);
    }
}
