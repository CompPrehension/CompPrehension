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
        MANAGE_COURSE_CONTENT(EnumSet.of(PermissionScopeKind.COURSE, PermissionScopeKind.EDUCATION_RESOURCE)),

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
        UNKNOWN(Set.of(), EnumSet.noneOf(PermissionScopeKind.class)),

        GLOBAL_ADMIN(
                EnumSet.complementOf(EnumSet.of(Permission.UNKNOWN)),
                EnumSet.of(PermissionScopeKind.GLOBAL)),

        GLOBAL_EXERCISE_AUTHOR(EnumSet.of(
                Permission.VIEW_EXERCISE,
                Permission.CREATE_EXERCISE,
                Permission.EDIT_EXERCISE,
                Permission.SOLVE_EXERCISE
        ), EnumSet.of(PermissionScopeKind.GLOBAL)),

        EDUCATION_RESOURCE_ADMIN(EnumSet.of(
                Permission.VIEW_COURSE,
                Permission.MANAGE_COURSE_CONTENT,
                Permission.VIEW_EXERCISE,
                Permission.CREATE_EXERCISE,
                Permission.EDIT_EXERCISE,
                Permission.DELETE_EXERCISE,
                Permission.SOLVE_EXERCISE
        ), EnumSet.of(PermissionScopeKind.EDUCATION_RESOURCE)),

        TEACHER(EnumSet.of(
                Permission.VIEW_COURSE,
                Permission.MANAGE_COURSE_CONTENT,
                Permission.VIEW_EXERCISE,
                Permission.CREATE_EXERCISE,
                Permission.EDIT_EXERCISE,
                Permission.DELETE_EXERCISE,
                Permission.SOLVE_EXERCISE
        ), EnumSet.of(PermissionScopeKind.COURSE)),

        ASSISTANT(EnumSet.of(
                Permission.VIEW_COURSE, Permission.VIEW_EXERCISE, Permission.SOLVE_EXERCISE
        ), EnumSet.of(PermissionScopeKind.COURSE)),

        STUDENT(
                EnumSet.of(Permission.SOLVE_EXERCISE),
                EnumSet.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.COURSE));

        private final Set<Permission> permissions;
        private final Set<PermissionScopeKind> allowedScopes;

        Role(Set<Permission> permissions, Set<PermissionScopeKind> allowedScopes) {
            this.permissions = Set.copyOf(permissions);
            this.allowedScopes = Set.copyOf(allowedScopes);
        }

        public boolean isAllowedIn(PermissionScopeKind scope) {
            return allowedScopes.contains(scope);
        }
    }
}
