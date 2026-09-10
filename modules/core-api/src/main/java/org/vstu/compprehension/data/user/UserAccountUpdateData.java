package org.vstu.compprehension.data.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.Language;

/**
 * Запрос на обновление пользователя.
 */
public record UserAccountUpdateData(
        @NotNull String email,
        @Nullable String fullName,
        @NotNull Language language,
        @Nullable String externalId,
        @Nullable String externalUserId) {
}
