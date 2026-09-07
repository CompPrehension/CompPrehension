package org.vstu.compprehension.data.user;

import org.jetbrains.annotations.NotNull;

/**
 * Привязка учётной записи к внешней системе.
 */
public record ExternalAccountData(long userId, long educationResourceId, @NotNull String externalId) {
}
