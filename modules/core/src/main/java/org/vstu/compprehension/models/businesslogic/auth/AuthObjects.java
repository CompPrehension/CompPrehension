package org.vstu.compprehension.models.businesslogic.auth;

import lombok.Getter;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;

import java.util.EnumSet;
import java.util.Set;

public final class AuthObjects {

    private AuthObjects() {
    }

    @Getter
    public enum SystemPermission implements Permission {
        UNKNOWN(EnumSet.noneOf(PermissionScopeKind.class)),

        VIEW_COURSE(allScopes()),
        MANAGE_COURSE_CONTENT(EnumSet.of(PermissionScopeKind.COURSE, PermissionScopeKind.EDUCATION_RESOURCE)),

        VIEW_EXERCISE(allScopes()),
        CREATE_EXERCISE(allScopes()),
        EDIT_EXERCISE(allScopes()),
        DELETE_EXERCISE(allScopes()),
        SOLVE_EXERCISE(allScopes());

        private final Set<PermissionScopeKind> allowedScopes;

        SystemPermission(Set<PermissionScopeKind> allowedScopes) {
            this.allowedScopes = Set.copyOf(allowedScopes);
        }

        @Override
        public String id() {
            return name();
        }

        @Override
        public boolean isAllowedIn(PermissionScopeKind scope) {
            return allowedScopes.contains(scope);
        }

        private static Set<PermissionScopeKind> allScopes() {
            return EnumSet.allOf(PermissionScopeKind.class);
        }
    }

    @Getter
    public enum SystemRole implements Role {
        UNKNOWN(Set.of(), EnumSet.noneOf(PermissionScopeKind.class)),

        GLOBAL_ADMIN(
                EnumSet.complementOf(EnumSet.of(SystemPermission.UNKNOWN)),
                EnumSet.of(PermissionScopeKind.GLOBAL)),

        GLOBAL_EXERCISE_AUTHOR(EnumSet.of(
                SystemPermission.VIEW_EXERCISE,
                SystemPermission.CREATE_EXERCISE,
                SystemPermission.EDIT_EXERCISE,
                SystemPermission.SOLVE_EXERCISE
        ), EnumSet.of(PermissionScopeKind.GLOBAL)),

        EDUCATION_RESOURCE_ADMIN(EnumSet.of(
                SystemPermission.VIEW_COURSE,
                SystemPermission.MANAGE_COURSE_CONTENT,
                SystemPermission.VIEW_EXERCISE,
                SystemPermission.CREATE_EXERCISE,
                SystemPermission.EDIT_EXERCISE,
                SystemPermission.DELETE_EXERCISE,
                SystemPermission.SOLVE_EXERCISE
        ), EnumSet.of(PermissionScopeKind.EDUCATION_RESOURCE)),

        TEACHER(EnumSet.of(
                SystemPermission.VIEW_COURSE,
                SystemPermission.MANAGE_COURSE_CONTENT,
                SystemPermission.VIEW_EXERCISE,
                SystemPermission.CREATE_EXERCISE,
                SystemPermission.EDIT_EXERCISE,
                SystemPermission.DELETE_EXERCISE,
                SystemPermission.SOLVE_EXERCISE
        ), EnumSet.of(PermissionScopeKind.COURSE)),

        ASSISTANT(EnumSet.of(
                SystemPermission.VIEW_COURSE, SystemPermission.VIEW_EXERCISE, SystemPermission.SOLVE_EXERCISE
        ), EnumSet.of(PermissionScopeKind.COURSE)),

        STUDENT(
                EnumSet.of(SystemPermission.SOLVE_EXERCISE),
                EnumSet.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.COURSE));

        private final Set<Permission> permissions;
        private final Set<PermissionScopeKind> allowedScopes;

        SystemRole(Set<? extends Permission> permissions, Set<PermissionScopeKind> allowedScopes) {
            this.permissions = Set.copyOf(permissions);
            this.allowedScopes = Set.copyOf(allowedScopes);
        }

        @Override
        public String id() {
            return name();
        }

        @Override
        public boolean isAllowedIn(PermissionScopeKind scope) {
            return allowedScopes.contains(scope);
        }
    }
}
