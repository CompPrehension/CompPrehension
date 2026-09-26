package org.vstu.compprehension.service.lti;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Подключённая LMS глазами инструмента — независимо от того, задана регистрация в env или создана динамически.
 * Ключ LMS — ровно одно из {@code platformJwksUrl} и {@code platformPublicKeyBase64}.
 */
public record LtiPlatform(
        @NotNull String toolKeyId,
        @NotNull String toolPrivateKeyPkcs8Base64,
        @NotNull String issuer,
        @NotNull String clientId,
        @NotNull String authorizationEndpoint,
        @NotNull String tokenEndpoint,
        @Nullable String platformJwksUrl,
        @Nullable String platformPublicKeyBase64) {
}
