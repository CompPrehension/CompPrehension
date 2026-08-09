package org.vstu.compprehension.models.businesslogic.auth;

import lombok.Getter;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;

import java.util.EnumSet;
import java.util.Set;

public final class AuthObjects {

    private AuthObjects() {
    }

    @Getter
    public enum Permission {
        UNKNOWN(EnumSet.noneOf(PermissionScopeKind.class)),

        VIEW_COURSE(allScopes()),
        CREATE_COURSE(EnumSet.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE)),
        EDIT_COURSE(allScopes()),
        DELETE_COURSE(allScopes()),

        VIEW_EXERCISE(allScopes()),
        CREATE_EXERCISE(allScopes()),
        EDIT_EXERCISE(allScopes()),
        DELETE_EXERCISE(allScopes()),
        SOLVE_EXERCISE(allScopes());

        private final Set<PermissionScopeKind> allowedScopes;

        Permission(Set<PermissionScopeKind> allowedScopes) {
            this.allowedScopes = Set.copyOf(allowedScopes);
        }

        public boolean isAllowedIn(PermissionScopeKind scope) {
            return allowedScopes.contains(scope);
        }

        private static Set<PermissionScopeKind> allScopes() {
            return EnumSet.allOf(PermissionScopeKind.class);
        }
    }

    @Getter
    public enum Role {
        UNKNOWN(Set.of()),

        GLOBAL_ADMIN(EnumSet.complementOf(EnumSet.of(Permission.UNKNOWN))),

        EDUCATION_RESOURCE_ADMIN(EnumSet.of(
                Permission.VIEW_COURSE,
                Permission.CREATE_COURSE,
                Permission.EDIT_COURSE,
                Permission.DELETE_COURSE,
                Permission.VIEW_EXERCISE,
                Permission.CREATE_EXERCISE,
                Permission.EDIT_EXERCISE,
                Permission.DELETE_EXERCISE,
                Permission.SOLVE_EXERCISE
        )),

        TEACHER(EnumSet.of(
                Permission.VIEW_COURSE,
                Permission.EDIT_COURSE,
                Permission.VIEW_EXERCISE,
                Permission.CREATE_EXERCISE,
                Permission.EDIT_EXERCISE,
                Permission.DELETE_EXERCISE,
                Permission.SOLVE_EXERCISE
        )),

        ASSISTANT(EnumSet.of(
                Permission.VIEW_COURSE, Permission.VIEW_EXERCISE, Permission.SOLVE_EXERCISE
        )),

        STUDENT(EnumSet.of(
                Permission.VIEW_COURSE, Permission.VIEW_EXERCISE, Permission.SOLVE_EXERCISE
        ));

        private final Set<Permission> permissions;

        Role(Set<Permission> permissions) {
            this.permissions = Set.copyOf(permissions);
        }
    }
}
