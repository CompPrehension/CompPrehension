package org.vstu.compprehension.data.lti;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.LtiRegistrationMethod;

import java.time.Instant;

/**
 * Инструмент CompPrehension, зарегистрированный в LMS.
 */
public record LtiRegistrationData(
        long id,
        long educationResourceId,
        @NotNull String educationResourceUrl,
        @NotNull String issuer,
        @NotNull String clientId,
        @Nullable String description,
        @Nullable String deploymentId,
        @NotNull LtiRegistrationMethod method,
        @NotNull String authorizationEndpoint,
        @NotNull String tokenEndpoint,
        @NotNull LtiPlatformKeyData platformKey,
        @NotNull Instant createdAt) {
}
