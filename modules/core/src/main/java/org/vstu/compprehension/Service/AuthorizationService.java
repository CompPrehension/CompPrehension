package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.businesslogic.auth.PermissionScopeKind;
import org.vstu.compprehension.models.businesslogic.auth.UserRoleAssignment;

import java.util.List;

public class AuthorizationService {
    public boolean isAuthorizedGlobal(long userId, String permissionName) {
        throw new RuntimeException();
    }

    public boolean isAuthorizedCourse(long userId, String permissionName, long courseId) {
        throw new RuntimeException();
    }

    public boolean isAuthorized(long userId, String permissionName, PermissionScopeKind scope, long scopeObjectId) {
        throw new RuntimeException();
    }

    public void addRoles(List<UserRoleAssignment> assignments) {
        throw new RuntimeException();
    }
}



