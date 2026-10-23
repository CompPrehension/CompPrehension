package org.vstu.compprehension.data.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.Language;

/**
 * Профиль пользователя, каким его передал источник входа.
 */
public record UserAccountUpdateData(
        @NotNull String email,
        @Nullable String fullName,
        @NotNull Language language) {
}
