package org.vstu.compprehension.service.lti;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.lti.LtiPlatformKeyData;

/**
 * Подключённая LMS.
 */
public record LtiPlatform(
        @NotNull String toolKeyId,
        @NotNull String toolPrivateKeyPkcs8Base64,
        @NotNull String issuer,
        @NotNull String clientId,
        long educationResourceId,
        @NotNull String authorizationEndpoint,
        @NotNull String tokenEndpoint,
        @NotNull LtiPlatformKeyData platformKey) {
}
