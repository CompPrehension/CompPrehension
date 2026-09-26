package org.vstu.compprehension.service.gradepassback;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exerciseattempt.GradePassbackTargetData;
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
    public boolean supports(@NotNull GradePassbackTargetData target) {
        return target.ltiLineitemUrl() != null;
    }

    @Override
    public void passGrade(@NotNull GradePassbackTargetData target, double grade, @NotNull Instant gradedAt) {
        String lineitemUrl = target.ltiLineitemUrl();
        String moodleBaseUrl = extractMoodleBaseUrl(lineitemUrl);

        String externalUserId = target.externalUserId();
        if (externalUserId == null || externalUserId.isBlank()) {
            throw new IllegalStateException(
                    "No externalUserId for user " + target.userId()
                            + " — пользователь должен войти через LTI до отправки оценки");
        }

        String accessToken;
        try {
            accessToken = tokenService.obtainAccessToken(moodleBaseUrl, SCORE_SCOPE);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not obtain AGS access token from " + moodleBaseUrl + ": " + ex.getMessage(), ex);
        }
        postScore(lineitemUrl, externalUserId, grade, gradedAt, accessToken);
    }

    /** Ответы 4xx/5xx RestTemplate сам превращает в исключение с кодом и телом ответа. */
    private void postScore(String lineitemUrl, String moodleUserId, double grade, Instant gradedAt,
                              String accessToken) {
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
                // LMS игнорирует оценку со временем старше уже полученной: повтор старой отправки не перезапишет новую.
                "timestamp", gradedAt.truncatedTo(ChronoUnit.SECONDS).toString()
        );

        HttpHeaders headers = new HttpHeaders();
        // LTI AGS spec requires this specific MIME type for score submission
        headers.setContentType(MediaType.parseMediaType("application/vnd.ims.lis.v1.score+json"));
        headers.setBearerAuth(accessToken);

        log.debug("Posting score to {}: userId={}, scoreGiven={}", scoresUrl, moodleUserId, grade);
        ResponseEntity<String> scoreResponse = restTemplate.postForEntity(
                URI.create(scoresUrl), new HttpEntity<>(scorePayload, headers), String.class);
        log.debug("AGS score response: status={}, body={}", scoreResponse.getStatusCode(), scoreResponse.getBody());
        if (!scoreResponse.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("AGS score was not accepted: " + scoreResponse.getStatusCode()
                    + " " + scoreResponse.getBody());
        }
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
