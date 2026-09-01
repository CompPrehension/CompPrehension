package org.vstu.compprehension.service.gradepassback;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.service.lti.LtiTokenService;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Grade passback через LTI AGS. Применяется, когда attempt создан через LTI-запуска
 */
@Service
@Order(1)
@Log4j2
public class LtiAgsGradePassbackStrategy implements GradePassbackStrategy {

    private static final String SCORE_SCOPE = "https://purl.imsglobal.org/spec/lti-ags/scope/score";

    private final RestTemplate restTemplate;
    private final LtiTokenService tokenService;

    public LtiAgsGradePassbackStrategy(RestTemplate restTemplate, LtiTokenService tokenService) {
        this.restTemplate = restTemplate;
        this.tokenService = tokenService;
    }

    @Override
    public boolean supports(ExerciseAttemptEntity attempt) {
        return attempt.getLtiLineitemUrl() != null;
    }

    @Override
    public boolean passGrade(ExerciseAttemptEntity attempt, double grade) {
        String lineitemUrl = attempt.getLtiLineitemUrl();
        try {
            String moodleBaseUrl = extractMoodleBaseUrl(lineitemUrl);

            String externalUserId = attempt.getUser().getExternalUserId();
            if (externalUserId == null || externalUserId.isBlank()) {
                throw new IllegalStateException(
                        "No externalUserId for user " + attempt.getUser().getId()
                                + " — пользователь должен войти через LTI до отправки оценки");
            }

            String accessToken = tokenService.obtainAccessToken(moodleBaseUrl, SCORE_SCOPE);
            return postScore(lineitemUrl, externalUserId, grade, accessToken);
        } catch (Exception e) {
            log.error("LTI AGS grade passback failed for attempt {}: {}", attempt.getId(), e.getMessage(), e);
            return false;
        }
    }

    private boolean postScore(String lineitemUrl, String moodleUserId, double grade, String accessToken) {
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

        log.debug("Posting score to {}: userId={}, scoreGiven={}", scoresUrl, moodleUserId, grade);
        ResponseEntity<String> scoreResponse = restTemplate.postForEntity(
                URI.create(scoresUrl), new HttpEntity<>(scorePayload, headers), String.class);
        log.debug("AGS score response: status={}, body={}", scoreResponse.getStatusCode(), scoreResponse.getBody());
        return scoreResponse.getStatusCode().is2xxSuccessful();
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
