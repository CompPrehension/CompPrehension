package org.vstu.compprehension.service.lti;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.config.LtiRegistrationsProperties;
import org.vstu.compprehension.config.LtiRegistrationsProperties.RegistrationWithName;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.services.LtiRegistrationDataService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Единая точка поиска подключённых LMS: сначала регистрации из env, затем динамические из БД.
 */
@Service
public class LtiRegistrationRegistry {

    private final LtiRegistrationsProperties ltiRegistrations;
    private final LtiRegistrationDataService ltiRegistrationService;

    public LtiRegistrationRegistry(LtiRegistrationsProperties ltiRegistrations,
                                   LtiRegistrationDataService ltiRegistrationService) {
        this.ltiRegistrations = ltiRegistrations;
        this.ltiRegistrationService = ltiRegistrationService;
    }

    public @NotNull Optional<LtiPlatform> findByIssuer(@NotNull String issuer) {
        var configured = ltiRegistrations.findByIssuerUrl(issuer);
        if (configured.isPresent()) {
            return configured.map(LtiRegistrationRegistry::toPlatform);
        }
        return ltiRegistrationService.findByIssuer(issuer).map(this::toPlatform);
    }

    public @NotNull LtiPlatform requireByIssuer(@NotNull String issuer) {
        return findByIssuer(issuer).orElseThrow(() -> new IllegalStateException(String.format(
                "LTI registration not configured for issuer %s", issuer)));
    }

    /** Приватные ключи инструмента по {@code kid}: их открытые части публикуются в JWKS. */
    public @NotNull Map<String, String> getToolPrivateKeysByKeyId() {
        var keys = new LinkedHashMap<String, String>();
        ltiRegistrations.getRegistrations().forEach((name, reg) -> keys.put(name, reg.getPrivateKeyPkcs8Base64()));
        if (ltiRegistrations.getToolPrivateKeyPkcs8Base64() != null) {
            keys.put(LtiRegistrationsProperties.TOOL_KEY_ID, ltiRegistrations.getToolPrivateKeyPkcs8Base64());
        }
        return keys;
    }

    private static @NotNull LtiPlatform toPlatform(@NotNull RegistrationWithName configured) {
        var reg = configured.registration();
        // Регистрации из env заводились под Moodle: его адреса авторизации и токенов стандартны.
        return new LtiPlatform(
                configured.name(),
                reg.getPrivateKeyPkcs8Base64(),
                reg.getIssuerUrl(),
                reg.getClientId(),
                reg.getIssuerUrl() + "/mod/lti/auth.php",
                reg.getIssuerUrl() + "/mod/lti/token.php",
                reg.getPlatformJwksUrl(),
                reg.getPlatformPublicKeyBase64());
    }

    private @NotNull LtiPlatform toPlatform(@NotNull LtiRegistrationData registration) {
        var toolKey = ltiRegistrations.getToolPrivateKeyPkcs8Base64();
        if (toolKey == null) {
            throw new IllegalStateException("LMS " + registration.issuer()
                    + " is registered dynamically, but compprehension.lti.tool-private-key-pkcs8-base64 is not set");
        }
        return new LtiPlatform(
                LtiRegistrationsProperties.TOOL_KEY_ID,
                toolKey,
                registration.issuer(),
                registration.clientId(),
                registration.authorizationEndpoint(),
                registration.tokenEndpoint(),
                registration.jwksUri(),
                null);
    }
}
