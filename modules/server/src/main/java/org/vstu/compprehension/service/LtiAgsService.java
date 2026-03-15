package org.vstu.compprehension.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.vstu.compprehension.Service.ExerciseAttemptService;
import org.vstu.compprehension.controllers.LtiController;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.events.AttemptCompletedEvent;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;

import java.net.URI;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
* Sends LTI AGS (Assignment and Grade Services) grade passback to Moodle.
 *
 * <p>Flow:
 * <ol>
 *   <li>Listens for {@link AttemptCompletedEvent} after the surrounding transaction commits.</li>
 *   <li>Obtains an OAuth2 access token from Moodle's LTI token endpoint using
 *       JWT-based {@code client_credentials} grant (LTI 1.3 standard).</li>
 *   <li>POSTs the score to the lineitem URL stored on the attempt.</li>
 * </ol>
 *
 * <p>Required properties (in application.properties):
 * <pre>
 *   lti.client-id=&lt;client-id registered in Moodle External Tools&gt;
 *   lti.private-key-pkcs8-base64=&lt;base64-encoded PKCS8 RSA private key&gt;
 * </pre>
 *
 * <p>Generate a key pair with:
 * <pre>
 *   openssl genrsa -out lti_private.pem 2048
 *   openssl pkcs8 -topk8 -nocrypt -in lti_private.pem -outform DER | base64 -w0 > lti_private_pkcs8.b64
 *   openssl rsa -in lti_private.pem -pubout > lti_public.pem
 * </pre>
 * Register the contents of lti_public.pem in Moodle's LTI tool configuration.
 */
@Service
@Log4j2
public class LtiAgsService {
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExerciseAttemptService exerciseAttemptService;
    private final MoodleDiscoveryGradeService moodleDiscoveryGradeService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${lti.client-id:}")
    private String ltiClientId;

    @Value("${lti.private-key-pkcs8-base64:}")
    private String privateKeyBase64;

    public LtiAgsService(ExerciseAttemptRepository exerciseAttemptRepository,
                         ExerciseAttemptService exerciseAttemptService,
                         MoodleDiscoveryGradeService moodleDiscoveryGradeService) {
        this.exerciseAttemptRepository = exerciseAttemptRepository;
        this.exerciseAttemptService = exerciseAttemptService;
        this.moodleDiscoveryGradeService = moodleDiscoveryGradeService;
    }

    /**
     * Handles attempt-completed events after the originating transaction commits.
     * Runs asynchronously so it does not block the user-facing request.
     *
     * <p>Two grade passback paths:
     * <ul>
     *   <li>LTI launch: {@code attempt.ltiLineitemUrl != null} → LTI AGS (existing flow)</li>
     *   <li>Keycloak login: {@code attempt.ltiLineitemUrl == null} → Moodle Web Services API
     *       via auto-discovery ({@link MoodleDiscoveryGradeService})</li>
     * </ul>
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAttemptCompleted(AttemptCompletedEvent event) {
        var attempt = exerciseAttemptRepository.findById(event.getAttemptId()).orElse(null);
        if (attempt == null) {
            return;
        }

        if (attempt.getLtiLineitemUrl() != null) {
            postViaLtiAgs(attempt);
        } else {
            moodleDiscoveryGradeService.postGradeIfEnrolled(attempt);
        }
    }

    /** Отправляет оценку через LTI AGS (стандартный путь для LTI-запусков). */
    private void postViaLtiAgs(ExerciseAttemptEntity attempt) {
        if (ltiClientId.isBlank() || privateKeyBase64.isBlank()) {
            log.warn("LTI AGS grade passback skipped for attempt {}: " +
                    "lti.client-id or lti.private-key-pkcs8-base64 not configured", attempt.getId());
            return;
        }

        try {
            var moodleBaseUrl = extractMoodleBaseUrl(attempt.getLtiLineitemUrl());
            var tokenEndpoint = moodleBaseUrl + "/mod/lti/token.php";
            var accessToken = obtainAccessToken(tokenEndpoint);
            var grade = exerciseAttemptService.calculateFinalGrade(attempt);
            // Use ltiUserId stored at attempt creation (independent of account merging)
            var moodleUserId = attempt.getLtiUserId() != null
                    ? attempt.getLtiUserId()
                    : extractMoodleUserId(attempt.getUser().getExternalId());
            postScore(attempt.getLtiLineitemUrl(), moodleUserId, grade, accessToken);
            log.info("Grade passback sent for attempt {}: userId={}, grade={}", attempt.getId(), moodleUserId, grade);
        } catch (Exception e) {
            log.error("Grade passback failed for attempt {}: {}", attempt.getId(), e.getMessage(), e);
        }
    }

    // ---- private helpers ----

    private String obtainAccessToken(String tokenEndpoint) throws Exception {
        var jwtAssertion = buildClientAssertionJwt(tokenEndpoint);

        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        form.add("client_assertion", jwtAssertion);
        form.add("scope", "https://purl.imsglobal.org/spec/lti-ags/scope/score");

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        var rawResponse = restTemplate.postForEntity(
                tokenEndpoint, new HttpEntity<>(form, headers), String.class);

        log.info("Token endpoint response: status={}, contentType={}, body={}",
                rawResponse.getStatusCode(),
                rawResponse.getHeaders().getContentType(),
                rawResponse.getBody());

        var body = rawResponse.getBody();
        if (body == null) {
            throw new RuntimeException("Empty response from token endpoint: " + tokenEndpoint);
        }

        var accessToken = objectMapper.readTree(body).path("access_token").asText(null);
        if (accessToken == null) {
            throw new RuntimeException("No access_token in Moodle AGS token response from " + tokenEndpoint + ": " + body);
        }
        return accessToken;
    }

    private String buildClientAssertionJwt(String tokenEndpoint) throws Exception {
        var keyBytes = Base64.getDecoder().decode(privateKeyBase64);
        var privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        var now = new Date();
        var claims = new JWTClaimsSet.Builder()
                .issuer(ltiClientId)
                .subject(ltiClientId)
                .audience(tokenEndpoint)
                .issueTime(now)
                .expirationTime(new Date(now.getTime() + 60_000))
                .jwtID(UUID.randomUUID().toString())
                .build();

        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(LtiController.KID).build(), claims);
        jwt.sign(new RSASSASigner(privateKey));
        return jwt.serialize();
    }

    private void postScore(String lineitemUrl, String moodleUserId, double grade, String accessToken) {
        // Insert /scores into the PATH before any query string (e.g. ?type_id=1)
        var uri = URI.create(lineitemUrl);
        var path = uri.getPath().replaceAll("/+$", "") + "/scores";
        var scoresUrl = uri.getScheme() + "://" + uri.getAuthority() + path
                + (uri.getQuery() != null ? "?" + uri.getQuery() : "");

        var scorePayload = Map.of(
                "userId", moodleUserId,
                "scoreGiven", grade,
                "scoreMaximum", 1.0,
                "activityProgress", "Completed",
                "gradingProgress", "FullyGraded",
                "timestamp", Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString()
        );

        var headers = new HttpHeaders();
        // LTI AGS spec requires this specific MIME type for score submission
        headers.setContentType(MediaType.parseMediaType("application/vnd.ims.lis.v1.score+json"));
        headers.setBearerAuth(accessToken);

        log.info("Posting score to {}: userId={}, scoreGiven={}", scoresUrl, moodleUserId, grade);
        var scoreResponse = restTemplate.postForEntity(scoresUrl, new HttpEntity<>(scorePayload, headers), String.class);
        log.info("AGS score response: status={}, body={}", scoreResponse.getStatusCode(), scoreResponse.getBody());
    }

    /**
     * Extracts the Moodle base URL from a lineitem URL.
     * Example: {@code http://moodle/mod/lti/services.php/2/lineitems/3/lineitem} → {@code http://moodle}
     */
    private String extractMoodleBaseUrl(String lineitemUrl) {
        var uri = URI.create(lineitemUrl);
        var path = uri.getPath();
        var modLtiIdx = path.indexOf("/mod/lti/");
        if (modLtiIdx >= 0) {
            path = path.substring(0, modLtiIdx);
        }
        var port = uri.getPort();
        var portStr = port > 0 ? ":" + port : "";
        return uri.getScheme() + "://" + uri.getHost() + portStr + path;
    }

    /**
     * Extracts the Moodle user ID from the externalId stored on the UserEntity.
     * externalId format: {@code {moodle_issuer_url}_{moodle_user_id}}
     */
    private String extractMoodleUserId(String externalId) {
        var lastUnderscore = externalId.lastIndexOf('_');
        if (lastUnderscore < 0) return externalId;
        return externalId.substring(lastUnderscore + 1);
    }
}
