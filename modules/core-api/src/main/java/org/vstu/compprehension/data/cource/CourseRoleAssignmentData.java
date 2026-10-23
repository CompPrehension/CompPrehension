package org.vstu.compprehension.data.cource;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.auth.Role;

public record CourseRoleAssignmentData(long id, long userId, long courseId, @NotNull Role role) {
}
