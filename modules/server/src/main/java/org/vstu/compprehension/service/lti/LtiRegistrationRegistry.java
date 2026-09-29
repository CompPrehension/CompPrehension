package org.vstu.compprehension.service.lti;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.config.LtiToolProperties;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.services.LtiRegistrationDataService;

import java.util.Optional;

/**
 * Подключённые LMS вместе с ключом инструмента.
 */
@Service
public class LtiRegistrationRegistry {

    private final LtiToolProperties toolProperties;
    private final LtiRegistrationDataService ltiRegistrationService;

    public LtiRegistrationRegistry(LtiToolProperties toolProperties, LtiRegistrationDataService ltiRegistrationService) {
        this.toolProperties = toolProperties;
        this.ltiRegistrationService = ltiRegistrationService;
    }

    public @NotNull Optional<LtiPlatform> findByIssuerAndClientId(@NotNull String issuer, @NotNull String clientId) {
        return ltiRegistrationService.findByIssuerAndClientId(issuer, clientId).map(this::toPlatform);
    }

    public @NotNull LtiPlatform requireByIssuerAndClientId(@NotNull String issuer, @NotNull String clientId) {
        return findByIssuerAndClientId(issuer, clientId).orElseThrow(() -> new IllegalStateException(String.format(
                "LTI tool with client_id %s is not registered for issuer %s", clientId, issuer)));
    }

    /** Без client_id инструмент LMS однозначен, только если он у неё единственный. */
    public @NotNull Optional<LtiPlatform> findSingleByIssuer(@NotNull String issuer) {
        var registrations = ltiRegistrationService.findAllByIssuer(issuer);
        return registrations.size() == 1 ? Optional.of(toPlatform(registrations.getFirst())) : Optional.empty();
    }

    public @NotNull Optional<String> findToolPrivateKey() {
        return Optional.ofNullable(toolProperties.getToolPrivateKeyPkcs8Base64());
    }

    private @NotNull LtiPlatform toPlatform(@NotNull LtiRegistrationData registration) {
        var toolKey = findToolPrivateKey().orElseThrow(() -> new IllegalStateException(
                "LMS " + registration.issuer() + " is registered, but compprehension.lti.tool-private-key-pkcs8-base64 is not set"));
        return new LtiPlatform(
                LtiToolProperties.TOOL_KEY_ID,
                toolKey,
                registration.issuer(),
                registration.clientId(),
                registration.educationResourceId(),
                registration.authorizationEndpoint(),
                registration.tokenEndpoint(),
                registration.platformKey());
    }
}
