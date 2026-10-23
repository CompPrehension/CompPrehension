package org.vstu.compprehension.frontend.dto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.LtiPlatformKeyType;

/**
 * Инструмент, зарегистрированный в LMS вручную: значения из настроек инструмента в LMS.
 */
public record NewLtiRegistrationDto(
        @NotNull String issuer,
        @NotNull String clientId,
        @Nullable String description,
        @Nullable String deploymentId,
        @NotNull String authorizationEndpoint,
        @NotNull String tokenEndpoint,
        @NotNull LtiPlatformKeyType platformKeyType,
        @NotNull String platformKey) {
}
