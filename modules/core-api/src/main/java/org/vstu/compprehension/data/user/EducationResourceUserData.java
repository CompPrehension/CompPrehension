package org.vstu.compprehension.data.user;

import org.jetbrains.annotations.NotNull;

/**
 * Учётная запись пользователя в образовательном ресурсе.
 */
public record EducationResourceUserData(long userId, long educationResourceId, @NotNull String externalId) {
}
