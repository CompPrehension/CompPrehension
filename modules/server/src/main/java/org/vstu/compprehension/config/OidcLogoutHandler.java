package org.vstu.compprehension.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OidcLogoutHandler implements LogoutHandler {
    private static final String END_SESSION_ENDPOINT = "end_session_endpoint";

    private final RestTemplate restTemplate;
    private final ClientRegistrationRepository clientRegistrations;

    @Override
    public void logout(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Authentication auth) {
        if (!(auth instanceof OAuth2AuthenticationToken token)) {
            return;
        }

        ClientRegistration registration = clientRegistrations.findByRegistrationId(token.getAuthorizedClientRegistrationId());
        if (registration != null && registration.getProviderDetails().getConfigurationMetadata().get(END_SESSION_ENDPOINT) instanceof String endSessionEndpoint) {
            logoutFromProvider(endSessionEndpoint, (OidcUser) token.getPrincipal());
        }
    }

    private void logoutFromProvider(String endSessionEndpoint, OidcUser user) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(endSessionEndpoint)
                .queryParam("id_token_hint", user.getIdToken().getTokenValue());

        ResponseEntity<String> logoutResponse = restTemplate.getForEntity(builder.toUriString(), String.class);
            /*
            if (logoutResponse.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfulley logged out from Keycloak");
            } else {
                logger.error("Could not propagate logout to Keycloak");
            }
            */
    }

}
