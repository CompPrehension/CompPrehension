package org.vstu.compprehension.data.cource;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.auth.Role;

/**
 * Уже выданная пользователю роль в курсе.
 * <p>
 * Идентификатор записи нужен для снятия роли: снимать по тройке
 * (пользователь, курс, роль) значит повторять в запросе тот же поиск, который уже
 * сделан при чтении текущего состояния.
 */
public record CourseRoleAssignmentData(long id, long userId, long courseId, @NotNull Role role) {
}
