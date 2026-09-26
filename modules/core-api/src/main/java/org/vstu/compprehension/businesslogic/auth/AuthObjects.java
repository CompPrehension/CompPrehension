package org.vstu.compprehension.businesslogic.auth;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

public final class AuthObjects {

    private AuthObjects() {
    }

    public enum SystemPermission implements Permission {
        UNKNOWN,

        VIEW_COURSE,
        LINK_POOL_EXERCISE_TO_COURSE,
        CREATE_LMS_ACTIVITY,

        VIEW_EXERCISE_LIST,
        VIEW_EXERCISE_CARD,
        SEARCH_QUESTION_BANK,
        CREATE_EXERCISE,
        EDIT_EXERCISE,
        DELETE_EXERCISE,
        SOLVE_EXERCISE,
        CREATE_DEBUG_ATTEMPT,
        VIEW_OTHER_ATTEMPTS,

        COPY_EXERCISE_TO_GLOBAL_POOL,
        VIEW_EXERCISE_USAGE,

        REGISTER_LMS;

        @Override
        public String id() {
            return name();
        }
    }

    public enum SystemCapability implements Capability {
        VIEW_GLOBAL_POOL,
        DEBUG_BANK_QUESTION;

        @Override
        public String id() {
            return name();
        }
    }

    @Getter
    public enum SystemRole implements Role {
        UNKNOWN(Set.of(), Set.of(), EnumSet.noneOf(PermissionScopeKind.class)),

        ADMIN(
                EnumSet.complementOf(EnumSet.of(SystemPermission.UNKNOWN)),
                EnumSet.allOf(SystemCapability.class),
                EnumSet.of(PermissionScopeKind.ROOT)),

        GLOBAL_EXERCISE_AUTHOR(EnumSet.of(
                SystemPermission.CREATE_EXERCISE,
                SystemPermission.EDIT_EXERCISE,
                SystemPermission.SOLVE_EXERCISE,
                SystemPermission.CREATE_DEBUG_ATTEMPT,
                SystemPermission.VIEW_OTHER_ATTEMPTS,
                SystemPermission.COPY_EXERCISE_TO_GLOBAL_POOL,
                SystemPermission.VIEW_EXERCISE_USAGE
        ), EnumSet.of(
                SystemCapability.VIEW_GLOBAL_POOL,
                SystemCapability.DEBUG_BANK_QUESTION
        ), EnumSet.of(PermissionScopeKind.GLOBAL)),

        EDUCATION_RESOURCE_ADMIN(EnumSet.of(
                SystemPermission.VIEW_COURSE,
                SystemPermission.LINK_POOL_EXERCISE_TO_COURSE,
                SystemPermission.CREATE_LMS_ACTIVITY,
                SystemPermission.VIEW_EXERCISE_LIST,
                SystemPermission.VIEW_EXERCISE_CARD,
                SystemPermission.SEARCH_QUESTION_BANK,
                SystemPermission.CREATE_EXERCISE,
                SystemPermission.EDIT_EXERCISE,
                SystemPermission.DELETE_EXERCISE,
                SystemPermission.SOLVE_EXERCISE,
                SystemPermission.CREATE_DEBUG_ATTEMPT,
                SystemPermission.VIEW_OTHER_ATTEMPTS
        ), EnumSet.of(
                SystemCapability.VIEW_GLOBAL_POOL,
                SystemCapability.DEBUG_BANK_QUESTION
        ), EnumSet.of(PermissionScopeKind.EDUCATION_RESOURCE)),

        TEACHER(EnumSet.of(
                SystemPermission.VIEW_COURSE,
                SystemPermission.LINK_POOL_EXERCISE_TO_COURSE,
                SystemPermission.CREATE_LMS_ACTIVITY,
                SystemPermission.VIEW_EXERCISE_LIST,
                SystemPermission.VIEW_EXERCISE_CARD,
                SystemPermission.SEARCH_QUESTION_BANK,
                SystemPermission.CREATE_EXERCISE,
                SystemPermission.EDIT_EXERCISE,
                SystemPermission.DELETE_EXERCISE,
                SystemPermission.SOLVE_EXERCISE,
                SystemPermission.CREATE_DEBUG_ATTEMPT,
                SystemPermission.VIEW_OTHER_ATTEMPTS
        ), EnumSet.of(
                SystemCapability.VIEW_GLOBAL_POOL,
                SystemCapability.DEBUG_BANK_QUESTION
        ), EnumSet.of(PermissionScopeKind.COURSE)),

        ASSISTANT(EnumSet.of(
                SystemPermission.VIEW_COURSE,
                SystemPermission.VIEW_EXERCISE_LIST,
                SystemPermission.VIEW_EXERCISE_CARD,
                SystemPermission.SEARCH_QUESTION_BANK,
                SystemPermission.SOLVE_EXERCISE,
                SystemPermission.VIEW_OTHER_ATTEMPTS
        ), Set.of(), EnumSet.of(PermissionScopeKind.COURSE)),

        STUDENT(
                EnumSet.of(SystemPermission.SOLVE_EXERCISE),
                Set.of(),
                EnumSet.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.COURSE));

        private final Set<Permission> permissions;
        private final Set<Capability> capabilities;
        private final Set<PermissionScopeKind> allowedScopes;

        SystemRole(Set<? extends Permission> permissions, Set<? extends Capability> capabilities,
                   Set<PermissionScopeKind> allowedScopes) {
            this.permissions = Set.copyOf(permissions);
            this.capabilities = Set.copyOf(capabilities);
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
