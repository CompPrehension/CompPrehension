package org.vstu.compprehension.adapters.lti;

import com.nimbusds.jwt.JWTClaimsSet;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Сервисный токен LMS для инструмента (OAuth2 {@code client_credentials} с {@code private_key_jwt}).
 */
@Component
public class LtiServiceTokenClient {

    /** На сколько раньше текущего момента выставляется {@code iat} подписываемых для LMS токенов. */
    public static final long ISSUED_AT_BACKDATE_MS = 60_000;

    private static final long ASSERTION_TTL_MS = 60_000;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final LtiToolKeyProvider toolKeys;

    public LtiServiceTokenClient(@NotNull RestTemplate restTemplate, @NotNull LtiToolKeyProvider toolKeys) {
        this.restTemplate = restTemplate;
        this.toolKeys = toolKeys;
    }

    public @NotNull String obtainAccessToken(@NotNull String tokenEndpoint, @NotNull String clientId, @NotNull String scope) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        form.add("client_assertion", buildClientAssertion(tokenEndpoint, clientId));
        form.add("scope", scope);

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        String body = restTemplate.postForObject(tokenEndpoint, new HttpEntity<>(form, headers), String.class);
        if (body == null) {
            throw new IllegalStateException("Empty response from token endpoint " + tokenEndpoint);
        }
        JsonNode response = JSON.readTree(body);
        String accessToken = response.path("access_token").asText(null);
        if (accessToken == null) {
            throw new IllegalStateException(String.format(
                    "No access_token in token response from %s, error=%s, error_description=%s",
                    tokenEndpoint, response.path("error").asText(null), response.path("error_description").asText(null)));
        }
        return accessToken;
    }

    private @NotNull String buildClientAssertion(@NotNull String tokenEndpoint, @NotNull String clientId) {
        var toolKey = toolKeys.findKey().orElseThrow(() -> new IllegalStateException(
                "LMS service token cannot be requested: LTI tool key is not set"));
        var now = new Date();
        var claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(tokenEndpoint)
                .issueTime(new Date(now.getTime() - ISSUED_AT_BACKDATE_MS))
                .expirationTime(new Date(now.getTime() + ASSERTION_TTL_MS))
                .jwtID(UUID.randomUUID().toString())
                .build();
        return toolKey.sign(claims);
    }
}
