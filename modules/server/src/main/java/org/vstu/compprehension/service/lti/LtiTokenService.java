package org.vstu.compprehension.service.lti;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.vstu.compprehension.config.LtiRegistrationsProperties;
import org.vstu.compprehension.config.LtiRegistrationsProperties.Registration;
import org.vstu.compprehension.config.LtiRegistrationsProperties.RegistrationWithName;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Единая точка получения OAuth2 {@code client_credentials} service-токена у LMS и подписи
 * client_assertion ключом LTI-регистрации (RS256, {@code kid} = имя регистрации). Используется
 * как для grade passback (AGS score), так и для чтения AGS line items при deep-linking - оба
 * сценария ходят к {@code <issuer>/mod/lti/token.php} тем же ключом, отличаясь только scope.
 */
@Service
@Log4j2
public class LtiTokenService {

    private static final long ASSERTION_TTL_MS = 60_000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LtiRegistrationsProperties ltiRegistrations;

    public LtiTokenService(RestTemplate restTemplate, LtiRegistrationsProperties ltiRegistrations) {
        this.restTemplate = restTemplate;
        this.ltiRegistrations = ltiRegistrations;
    }

    /**
     * Получает access token у LMS по её issuer URL для запрошенного scope. Регистрация ищется по
     * issuer URL; token endpoint — {@code issuerUrl + /mod/lti/token.php}.
     */
    public String obtainAccessToken(String issuerUrl, String scope) throws Exception {
        RegistrationWithName regWithName = requireRegistration(issuerUrl);
        String tokenEndpoint = String.format("%s/mod/lti/token.php", issuerUrl);
        String assertion = buildClientAssertionJwt(tokenEndpoint, regWithName.registration(), regWithName.name());

        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        form.add("client_assertion", assertion);
        form.add("scope", scope);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        ResponseEntity<String> response = restTemplate.postForEntity(
                tokenEndpoint, new HttpEntity<>(form, headers), String.class);

        String body = response.getBody();
        if (body == null) {
            throw new RuntimeException(String.format("Empty response from token endpoint: %s", tokenEndpoint));
        }
        JsonNode tokenResponse = objectMapper.readTree(body);
        String accessToken = tokenResponse.path("access_token").asText(null);
        if (accessToken == null) {
            String error = tokenResponse.path("error").asText(null);
            String errorDescription = tokenResponse.path("error_description").asText(null);
            throw new RuntimeException(String.format(
                    "No access_token in token response from %s, error=%s, error_description=%s",
                    tokenEndpoint, error, errorDescription));
        }
        return accessToken;
    }

    public RegistrationWithName requireRegistration(String issuerUrl) {
        return ltiRegistrations.findByIssuerUrl(issuerUrl)
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "LTI registration not configured for issuer %s — добавьте compprehension.lti.registrations.<name>.* в env",
                        issuerUrl)));
    }

    /** Приватный RSA-ключ регистрации из PKCS8 base64. */
    public RSAPrivateKey loadPrivateKey(Registration reg) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(reg.getPrivateKeyPkcs8Base64());
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private String buildClientAssertionJwt(String tokenEndpoint, Registration reg, String kid) throws Exception {
        Date now = new Date();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(reg.getClientId())
                .subject(reg.getClientId())
                .audience(tokenEndpoint)
                .issueTime(now)
                .expirationTime(new Date(now.getTime() + ASSERTION_TTL_MS))
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(), claims);
        jwt.sign(new RSASSASigner(loadPrivateKey(reg)));
        return jwt.serialize();
    }
}
