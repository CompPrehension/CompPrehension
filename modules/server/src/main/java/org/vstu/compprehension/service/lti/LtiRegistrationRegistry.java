package org.vstu.compprehension.service.lti;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.services.LtiRegistrationDataService;

import java.util.Optional;

/**
 * Подключённые LMS.
 */
@Service
public class LtiRegistrationRegistry {

    private final LtiRegistrationDataService ltiRegistrationService;

    public LtiRegistrationRegistry(LtiRegistrationDataService ltiRegistrationService) {
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

    private @NotNull LtiPlatform toPlatform(@NotNull LtiRegistrationData registration) {
        return new LtiPlatform(
                registration.issuer(),
                registration.clientId(),
                registration.educationResourceId(),
                registration.authorizationEndpoint(),
                registration.tokenEndpoint(),
                registration.platformKey());
    }
}
