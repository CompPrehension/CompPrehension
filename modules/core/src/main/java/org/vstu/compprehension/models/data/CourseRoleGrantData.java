package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;

import org.vstu.compprehension.models.businesslogic.auth.Role;

/**
 * Роль, которую нужно выдать пользователю в курсе.
 * <p>
 * Отдельно от {@link CourseRoleAssignmentData}: у выдачи ещё нет идентификатора записи,
 * а роль здесь обязательна — «роли нет» выражается отсутствием выдачи, а не пустым полем.
 */
public record CourseRoleGrantData(long userId, long courseId, @NotNull Role role) {
}
