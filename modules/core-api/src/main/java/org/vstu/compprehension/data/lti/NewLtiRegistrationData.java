package org.vstu.compprehension.data.lti;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record NewLtiRegistrationData(
        @NotNull String lmsUrl,
        @NotNull String issuer,
        @NotNull String clientId,
        @Nullable String deploymentId,
        @NotNull String authorizationEndpoint,
        @NotNull String tokenEndpoint,
        @NotNull LtiPlatformKeyData platformKey) {
}
