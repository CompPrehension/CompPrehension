package org.vstu.compprehension.service.gradepassback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.vstu.compprehension.config.LtiRegistrationsProperties;
import org.vstu.compprehension.config.LtiRegistrationsProperties.Registration;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;

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
 * Grade passback через LTI AGS. Применяется, когда attempt создан через LTI-запуска
 */
@Service
@Order(1)
@Log4j2
public class LtiAgsGradePassbackStrategy implements GradePassbackStrategy {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LtiRegistrationsProperties ltiRegistrations;

    public LtiAgsGradePassbackStrategy(LtiRegistrationsProperties ltiRegistrations) {
        this.ltiRegistrations = ltiRegistrations;
    }

    @Override
    public boolean supports(ExerciseAttemptEntity attempt) {
        return attempt.getLtiLineitemUrl() != null;
    }

    @Override
    public void passGrade(ExerciseAttemptEntity attempt) {
        String lineitemUrl = attempt.getLtiLineitemUrl();
        try {
            String moodleBaseUrl = extractMoodleBaseUrl(lineitemUrl);

            var regWithName = ltiRegistrations.findByIssuerUrl(moodleBaseUrl)
                    .orElseThrow(() -> new IllegalStateException(
                            "LTI registration not configured for issuer " + moodleBaseUrl
                                    + " — добавьте compprehension.lti.registrations.<name>.* в env"));
            String kid = regWithName.name();
            Registration reg = regWithName.registration();

            String externalUserId = attempt.getUser().getExternalUserId();
            if (externalUserId == null || externalUserId.isBlank()) {
                throw new IllegalStateException(
                        "No externalUserId for user " + attempt.getUser().getId()
                                + " — пользователь должен войти через LTI до отправки оценки");
            }

            String tokenEndpoint = moodleBaseUrl + "/mod/lti/token.php";
            String accessToken = obtainAccessToken(tokenEndpoint, reg, kid);
            double grade = calculateFinalGrade(attempt);
            postScore(lineitemUrl, externalUserId, grade, accessToken);
            log.info("LTI AGS grade passback sent for attempt {}: userId={}, grade={}",
                    attempt.getId(), externalUserId, grade);
        } catch (Exception e) {
            log.error("LTI AGS grade passback failed for attempt {}: {}", attempt.getId(), e.getMessage(), e);
        }
    }

    private String obtainAccessToken(String tokenEndpoint, Registration reg, String kid) throws Exception {
        String jwtAssertion = buildClientAssertionJwt(tokenEndpoint, reg, kid);

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

    private String buildClientAssertionJwt(String tokenEndpoint, Registration reg, String kid) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(reg.getPrivateKeyPkcs8Base64());
        RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        Date now = new Date();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(reg.getClientId())
                .subject(reg.getClientId())
                .audience(tokenEndpoint)
                .issueTime(now)
                .expirationTime(new Date(now.getTime() + 60_000))
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(), claims);
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

    /** {@code http://moodle/mod/lti/services.php/2/lineitems/3/lineitem} -> {@code http://moodle}. */
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
}
