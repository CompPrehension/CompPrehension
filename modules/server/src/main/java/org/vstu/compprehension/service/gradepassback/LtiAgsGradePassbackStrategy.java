package org.vstu.compprehension.service.gradepassback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.vstu.compprehension.Service.ExerciseAttemptService;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.service.LtiConstants;

import java.net.URI;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Стратегия grade passback через LTI AGS (Assignment and Grade Services).
 *
 * <p>Применяется когда attempt создан через LTI-запуск (ltiLineitemUrl != null).
 *
 * <p>Требуемые настройки в application.properties:
 * <pre>
 *   lti.client-id=&lt;client-id registered in Moodle External Tools&gt;
 *   lti.private-key-pkcs8-base64=&lt;base64-encoded PKCS8 RSA private key&gt;
 * </pre>
 */
@Service
@Order(1)
@Log4j2
public class LtiAgsGradePassbackStrategy implements GradePassbackStrategy {

    private final ExerciseAttemptService exerciseAttemptService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${lti.client-id:}")
    private String ltiClientId;

    @Value("${lti.private-key-pkcs8-base64:}")
    private String privateKeyBase64;

    public LtiAgsGradePassbackStrategy(ExerciseAttemptService exerciseAttemptService) {
        this.exerciseAttemptService = exerciseAttemptService;
    }

    @Override
    public boolean supports(ExerciseAttemptEntity attempt) {
        return attempt.getLtiLineitemUrl() != null;
    }

    @Override
    public void passGrade(ExerciseAttemptEntity attempt) {
        if (ltiClientId.isBlank() || privateKeyBase64.isBlank()) {
            log.warn("LTI AGS grade passback skipped for attempt {}: " +
                    "lti.client-id or lti.private-key-pkcs8-base64 not configured", attempt.getId());
            return;
        }

        try {
            String moodleBaseUrl = extractMoodleBaseUrl(attempt.getLtiLineitemUrl());
            String tokenEndpoint = moodleBaseUrl + "/mod/lti/token.php";
            String accessToken = obtainAccessToken(tokenEndpoint);
            double grade = exerciseAttemptService.calculateFinalGrade(attempt);
            // Use ltiUserId stored at attempt creation (independent of account merging)
            String moodleUserId = attempt.getLtiUserId() != null
                    ? attempt.getLtiUserId()
                    : extractMoodleUserId(attempt.getUser().getExternalId());
            postScore(attempt.getLtiLineitemUrl(), moodleUserId, grade, accessToken);
            log.info("LTI AGS grade passback sent for attempt {}: userId={}, grade={}",
                    attempt.getId(), moodleUserId, grade);
        } catch (Exception e) {
            log.error("LTI AGS grade passback failed for attempt {}: {}", attempt.getId(), e.getMessage(), e);
        }
    }

    // ---- private helpers ----

    private String obtainAccessToken(String tokenEndpoint) throws Exception {
        String jwtAssertion = buildClientAssertionJwt(tokenEndpoint);

        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        form.add("client_assertion", jwtAssertion);
        form.add("scope", "https://purl.imsglobal.org/spec/lti-ags/scope/score");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                tokenEndpoint, new HttpEntity<>(form, headers), String.class);

        log.info("Token endpoint response: status={}, contentType={}, body={}",
                rawResponse.getStatusCode(),
                rawResponse.getHeaders().getContentType(),
                rawResponse.getBody());

        String body = rawResponse.getBody();
        if (body == null) {
            throw new RuntimeException("Empty response from token endpoint: " + tokenEndpoint);
        }

        String accessToken = objectMapper.readTree(body).path("access_token").asText(null);
        if (accessToken == null) {
            throw new RuntimeException("No access_token in Moodle AGS token response from " + tokenEndpoint + ": " + body);
        }
        return accessToken;
    }

    private String buildClientAssertionJwt(String tokenEndpoint) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
        RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        Date now = new Date();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ltiClientId)
                .subject(ltiClientId)
                .audience(tokenEndpoint)
                .issueTime(now)
                .expirationTime(new Date(now.getTime() + 60_000))
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(LtiConstants.KID).build(), claims);
        jwt.sign(new RSASSASigner(privateKey));
        return jwt.serialize();
    }

    private void postScore(String lineitemUrl, String moodleUserId, double grade, String accessToken) {
        // Insert /scores into the PATH before any query string (e.g. ?type_id=1)
        URI uri = URI.create(lineitemUrl);
        String path = uri.getPath().replaceAll("/+$", "") + "/scores";
        String scoresUrl = uri.getScheme() + "://" + uri.getAuthority() + path
                + (uri.getQuery() != null ? "?" + uri.getQuery() : "");

        Map<String, Object> scorePayload = Map.of(
                "userId", moodleUserId,
                "scoreGiven", grade,
                "scoreMaximum", 1.0,
                "activityProgress", "Completed",
                "gradingProgress", "FullyGraded",
                "timestamp", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
        );

        HttpHeaders headers = new HttpHeaders();
        // LTI AGS spec requires this specific MIME type for score submission
        headers.setContentType(MediaType.parseMediaType("application/vnd.ims.lis.v1.score+json"));
        headers.setBearerAuth(accessToken);

        log.info("Posting score to {}: userId={}, scoreGiven={}", scoresUrl, moodleUserId, grade);
        ResponseEntity<String> scoreResponse = restTemplate.postForEntity(
                scoresUrl, new HttpEntity<>(scorePayload, headers), String.class);
        log.info("AGS score response: status={}, body={}", scoreResponse.getStatusCode(), scoreResponse.getBody());
    }

    /**
     * Extracts the Moodle base URL from a lineitem URL.
     * Example: {@code http://moodle/mod/lti/services.php/2/lineitems/3/lineitem} → {@code http://moodle}
     */
    private String extractMoodleBaseUrl(String lineitemUrl) {
        URI uri = URI.create(lineitemUrl);
        String path = uri.getPath();
        int modLtiIdx = path.indexOf("/mod/lti/");
        if (modLtiIdx >= 0) {
            path = path.substring(0, modLtiIdx);
        }
        int port = uri.getPort();
        String portStr = port > 0 ? ":" + port : "";
        return uri.getScheme() + "://" + uri.getHost() + portStr + path;
    }

    /**
     * Extracts the Moodle user ID from the externalId stored on the UserEntity.
     * externalId format: {@code {moodle_issuer_url}_{moodle_user_id}}
     */
    private String extractMoodleUserId(String externalId) {
        int lastUnderscore = externalId.lastIndexOf('_');
        if (lastUnderscore < 0) return externalId;
        return externalId.substring(lastUnderscore + 1);
    }
}
