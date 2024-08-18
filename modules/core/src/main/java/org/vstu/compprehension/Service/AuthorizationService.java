package org.vstu.compprehension.Service;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.businesslogic.auth.PermissionScopeKind;
import org.vstu.compprehension.models.businesslogic.auth.UserRoleAssignment;

import java.util.List;

@Service
public class AuthorizationService {
    public boolean isAuthorizedGlobal(long userId, String permissionName) {
        return isAuthorized(userId, permissionName, PermissionScopeKind.GLOBAL, null);
    }

    public boolean isAuthorizedCourse(long userId, String permissionName, long courseId) {
        return isAuthorized(userId, permissionName, PermissionScopeKind.COURSE, courseId);
    }

    public boolean isAuthorized(long userId, String permissionName, PermissionScopeKind scope, @Nullable Long scopeObjectId) {
        // TODO implement
        return true;
    }
}



