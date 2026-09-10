package org.vstu.compprehension.data.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.Language;

/**
 * Учётная запись.
 */
public record UserAccountData(
        long id,
        @Nullable String firstName,
        @Nullable String lastName,
        @NotNull String email,
        @NotNull Language language,
        @Nullable String externalId,
        @Nullable String externalUserId) {
}
