package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.cource.CourseRoleAssignmentData;
import org.vstu.compprehension.entities.role.RoleUserAssignmentEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.utils.Strict;

@Component
class CourseRoleAssignmentMapper implements Mapper<RoleUserAssignmentEntity, CourseRoleAssignmentData> {

    @Override
    public @NotNull CourseRoleAssignmentData map(@NotNull RoleUserAssignmentEntity source) {
        long id = Strict.required(source.getId(), "id", "role assignment");
        String owner = "role assignment " + id;
        var user = Strict.required(source.getUser(), "user", owner);
        var role = Strict.required(source.getRole(), "role", owner);
        var scope = Strict.required(source.getPermissionScope(), "permissionScope", owner);
        return new CourseRoleAssignmentData(
                id,
                Strict.required(user.getId(), "user.id", owner),
                Strict.required(scope.getScopeItemId(), "permissionScope.scopeItemId", owner),
                Strict.required(role.getName(), "role.name", owner));
    }
}
