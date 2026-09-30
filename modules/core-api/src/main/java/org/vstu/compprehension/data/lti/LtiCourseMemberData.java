package org.vstu.compprehension.data.lti;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Участник курса по данным LMS.
 *
 * @param userId {@code sub} пользователя в LMS — тот же, что приходит в его LTI-запуске
 * @param active {@code false} для отчисленных и удалённых из курса
 */
public record LtiCourseMemberData(
        @NotNull String userId,
        @NotNull List<String> roles,
        boolean active) {
}
