package org.vstu.compprehension.service.lti;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * id_token запуска с подписью, проверенной ключом регистрации {@code platform}.
 */
public record VerifiedLtiIdToken(@NotNull Jwt idToken, @NotNull LtiPlatform platform) {
}
