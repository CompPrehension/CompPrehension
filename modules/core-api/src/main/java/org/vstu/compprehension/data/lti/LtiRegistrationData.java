package org.vstu.compprehension.data.lti;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public record LtiRegistrationData(
        long id,
        long educationResourceId,
        @NotNull String educationResourceUrl,
        @NotNull String issuer,
        @NotNull String clientId,
        @Nullable String deploymentId,
        @NotNull String authorizationEndpoint,
        @NotNull String tokenEndpoint,
        @NotNull String jwksUri,
        @NotNull Instant createdAt) {
}
