package org.vstu.compprehension.service.lti;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.vstu.compprehension.common.LmsUrlHelper;
import org.vstu.compprehension.config.LtiRegistrationsProperties;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.data.lti.NewLtiRegistrationData;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.services.LtiRegistrationDataService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LTI Dynamic Registration (IMS): LMS открывает нашу ссылку регистрации, мы берём её настройки
 * ({@code openid_configuration}), сообщаем ей свои адреса и ключи и сохраняем выданный client_id.
 */
@Service
@Log4j2
public class LtiDynamicRegistrationService {

    private static final String TOOL_CONFIGURATION = "https://purl.imsglobal.org/spec/lti-tool-configuration";
    private static final String PLATFORM_CONFIGURATION = "https://purl.imsglobal.org/spec/lti-platform-configuration";
    private static final String AGS_SCOPES = String.join(" ",
            "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem",
            "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem.readonly",
            "https://purl.imsglobal.org/spec/lti-ags/scope/score");
    private static final String TOOL_NAME = "CompPrehension";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LtiRegistrationsProperties ltiRegistrations;
    private final LtiRegistrationDataService ltiRegistrationService;

    public LtiDynamicRegistrationService(RestTemplate restTemplate,
                                         LtiRegistrationsProperties ltiRegistrations,
                                         LtiRegistrationDataService ltiRegistrationService) {
        this.restTemplate = restTemplate;
        this.ltiRegistrations = ltiRegistrations;
        this.ltiRegistrationService = ltiRegistrationService;
    }

    public void ensureConfigured() {
        if (ltiRegistrations.getToolBaseUrl() == null || ltiRegistrations.getToolPrivateKeyPkcs8Base64() == null) {
            throw new IllegalStateException("LTI dynamic registration is not configured: set "
                    + "compprehension.lti.tool-base-url and compprehension.lti.tool-private-key-pkcs8-base64");
        }
    }

    public @NotNull LtiRegistrationData register(@NotNull String inviteToken,
                                                 @NotNull String openidConfigurationUrl,
                                                 @Nullable String registrationToken) {
        ensureConfigured();
        // Ссылку проверяем до любых запросов наружу: адреса LMS приходят из запроса, и без годной ссылки
        // сервер не должен ходить по ним.
        ltiRegistrationService.ensureInviteUsable(inviteToken);

        JsonNode platformConfiguration = fetchJson(openidConfigurationUrl);
        String issuer = requireText(platformConfiguration, "issuer");
        if (!openidConfigurationUrl.startsWith(issuer.endsWith("/") ? issuer : issuer + "/")) {
            throw new SecurityException(String.format(
                    "openid_configuration %s does not belong to issuer %s", openidConfigurationUrl, issuer));
        }
        if (ltiRegistrationService.findByIssuer(issuer).isPresent()
                || ltiRegistrations.findByIssuerUrl(issuer).isPresent()) {
            throw new IllegalStateException(String.format(
                    "LMS %s is already registered: delete the existing registration first", issuer));
        }
        String lmsUrl = LmsUrlHelper.toCanonicalLmsUrl(issuer);
        if (lmsUrl == null) {
            throw new SecurityException("Invalid LMS issuer " + issuer);
        }

        JsonNode toolRegistration = postRegistration(
                requireText(platformConfiguration, "registration_endpoint"), registrationToken);
        String deploymentId = toolRegistration.path(TOOL_CONFIGURATION).path("deployment_id").asText(null);

        var registration = ltiRegistrationService.registerByInvite(inviteToken, new NewLtiRegistrationData(
                lmsUrl,
                EducationResourceType.fromString(
                        platformConfiguration.path(PLATFORM_CONFIGURATION).path("product_family_code").asText(null)),
                issuer,
                requireText(toolRegistration, "client_id"),
                deploymentId,
                requireText(platformConfiguration, "authorization_endpoint"),
                requireText(platformConfiguration, "token_endpoint"),
                requireText(platformConfiguration, "jwks_uri")));
        log.info("LMS {} registered dynamically, client_id {}", issuer, registration.clientId());
        return registration;
    }

    private @NotNull JsonNode postRegistration(@NotNull String registrationEndpoint, @Nullable String registrationToken) {
        String baseUrl = ltiRegistrations.getToolBaseUrl();
        String launchUrl = baseUrl + "/lti/1_3/launch";

        Map<String, Object> toolConfiguration = new LinkedHashMap<>();
        toolConfiguration.put("domain", URI.create(baseUrl).getAuthority());
        toolConfiguration.put("target_link_uri", launchUrl);
        toolConfiguration.put("claims", List.of("iss", "sub", "name", "given_name", "family_name", "email"));
        toolConfiguration.put("messages", List.of(Map.of(
                "type", "LtiDeepLinkingRequest",
                "target_link_uri", launchUrl,
                "label", TOOL_NAME)));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("application_type", "web");
        body.put("response_types", List.of("id_token"));
        body.put("grant_types", List.of("implicit", "client_credentials"));
        body.put("initiate_login_uri", baseUrl + "/lti/1_3/login");
        body.put("redirect_uris", List.of(launchUrl));
        body.put("client_name", TOOL_NAME);
        body.put("jwks_uri", baseUrl + "/lti/1_3/jwks");
        body.put("token_endpoint_auth_method", "private_key_jwt");
        body.put("scope", AGS_SCOPES);
        body.put(TOOL_CONFIGURATION, toolConfiguration);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (registrationToken != null && !registrationToken.isBlank()) {
            headers.setBearerAuth(registrationToken);
        }
        String response = restTemplate.postForObject(
                registrationEndpoint, new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);
        return parseJson(response, registrationEndpoint);
    }

    private @NotNull JsonNode fetchJson(@NotNull String url) {
        return parseJson(restTemplate.getForObject(url, String.class), url);
    }

    private @NotNull JsonNode parseJson(@Nullable String body, @NotNull String url) {
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Empty response from " + url);
        }
        return objectMapper.readTree(body);
    }

    private static @NotNull String requireText(@NotNull JsonNode node, @NotNull String field) {
        String value = node.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("LMS response has no " + field);
        }
        return value;
    }
}
