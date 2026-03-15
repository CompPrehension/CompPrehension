package org.vstu.compprehension.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * HTTP-клиент для Moodle Web Services REST API.
 *
 * <p>Используется для grade passback пользователей, аутентифицированных через Keycloak (не LTI).
 *
 * <p>Настройка в application.properties:
 * <pre>
 *   moodle.base-url=http://localhost:8081
 *   moodle.webservice-token=&lt;admin web service token&gt;
 * </pre>
 *
 * <p>Токен создаётся в Moodle: Admin → Server → Web services → Manage tokens → Create token.
 * Сервис-аккаунт должен иметь права на функции:
 * core_user_get_users_by_field, core_enrol_get_users_courses,
 * mod_lti_get_ltis_by_courses, core_grades_update_grades.
 *
 * <p>// TECH DEBT: wstoken — долгоживущий admin-токен в конфиге.
 * В продакшне заменить на сервис-аккаунт с минимальными capabilities.
 */
@Service
@Log4j2
public class MoodleApiClient {

    @Value("${moodle.base-url:}")
    private String moodleBaseUrl;

    @Value("${moodle.webservice-token:}")
    private String wsToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Возвращает true если интеграция с Moodle Web Services настроена. */
    public boolean isConfigured() {
        return !moodleBaseUrl.isBlank() && !wsToken.isBlank();
    }

    /**
     * Ищет пользователя Moodle по email.
     * Вызывает core_user_get_users_by_field.
     *
     * @return Moodle user ID (строка), или empty если не найден
     */
    public Optional<String> findUserIdByEmail(String email) {
        var url = buildUrl("core_user_get_users_by_field",
                "field", "email",
                "values[0]", email);
        var json = callJson(url);
        if (json == null || !json.isArray() || json.isEmpty()) return Optional.empty();
        var idNode = json.get(0).path("id");
        return idNode.isMissingNode() ? Optional.empty() : Optional.of(idNode.asText());
    }

    /**
     * Возвращает список ID курсов, в которых зачислен пользователь.
     * Вызывает core_enrol_get_users_courses.
     */
    public List<Long> getEnrolledCourseIds(String moodleUserId) {
        var url = buildUrl("core_enrol_get_users_courses", "userid", moodleUserId);
        var json = callJson(url);
        if (json == null || !json.isArray()) return List.of();

        var result = new ArrayList<Long>();
        for (var item : json) {
            var id = item.path("id");
            if (!id.isMissingNode()) result.add(id.asLong());
        }
        return result;
    }

    /**
     * Возвращает LTI-активности для указанных курсов.
     * Вызывает mod_lti_get_ltis_by_courses.
     */
    public List<MoodleLtiActivity> getLtiActivities(List<Long> courseIds) {
        if (courseIds.isEmpty()) return List.of();

        var builder = UriComponentsBuilder.fromHttpUrl(moodleBaseUrl + "/webservice/rest/server.php")
                .queryParam("wstoken", wsToken)
                .queryParam("wsfunction", "mod_lti_get_ltis_by_courses")
                .queryParam("moodlewsrestformat", "json");
        for (int i = 0; i < courseIds.size(); i++) {
            builder.queryParam("courseids[" + i + "]", courseIds.get(i));
        }

        var json = callJson(builder.toUriString());
        if (json == null) return List.of();

        var ltis = json.path("ltis");
        if (!ltis.isArray()) return List.of();

        var result = new ArrayList<MoodleLtiActivity>();
        for (var item : ltis) {
            var activity = new MoodleLtiActivity();
            activity.setCmid(item.path("coursemodule").asLong());
            activity.setCourseId(item.path("course").asLong());
            activity.setToolUrl(item.path("toolurl").asText(""));
            activity.setCustomParams(item.path("instructorcustomparameters").asText(""));
            result.add(activity);
        }
        return result;
    }

    /**
     * Выставляет оценку через core_grades_update_grades.
     *
     * @param courseId    Moodle course ID
     * @param cmid        course_modules.id (activityid в core_grades_update_grades)
     * @param moodleUserId Moodle internal user ID
     * @param rawGrade    оценка в шкале activity (обычно 0–100)
     */
    public void updateGrade(long courseId, long cmid, String moodleUserId, double rawGrade) {
        var url = buildUrl("core_grades_update_grades",
                "source", "compph",
                "courseid", String.valueOf(courseId),
                "component", "mod_lti",
                "activityid", String.valueOf(cmid),
                "itemnumber", "0",
                "grades[0][studentid]", moodleUserId,
                "grades[0][grade]", String.valueOf(rawGrade));
        var json = callJson(url);
        // core_grades_update_grades returns: {"result":true,"warnings":[]}
        if (json != null && !json.path("result").asBoolean(true)) {
            throw new RuntimeException("core_grades_update_grades returned result=false: " + json);
        }
        log.debug("core_grades_update_grades response: {}", json);
    }

    // ---- Inner types ----

    @Data
    public static class MoodleLtiActivity {
        /** course_modules.id — передаётся как activityid в core_grades_update_grades */
        long cmid;
        /** Moodle course ID */
        long courseId;
        /** URL External Tool, например https://trainer/lti/1_3/exercise?id=3 */
        String toolUrl;
        /** instructorcustomparameters, например "exercise_id=3\nkey=val" */
        String customParams;
    }

    // ---- private helpers ----

    private String buildUrl(String wsfunction, String... params) {
        var builder = UriComponentsBuilder.fromHttpUrl(moodleBaseUrl + "/webservice/rest/server.php")
                .queryParam("wstoken", wsToken)
                .queryParam("wsfunction", wsfunction)
                .queryParam("moodlewsrestformat", "json");
        for (int i = 0; i < params.length - 1; i += 2) {
            builder.queryParam(params[i], params[i + 1]);
        }
        return builder.toUriString();
    }

    private JsonNode callJson(String url) {
        try {
            var body = restTemplate.getForEntity(URI.create(url), String.class).getBody();
            if (body == null) return null;
            var node = objectMapper.readTree(body);
            if (node.has("exception")) {
                log.warn("Moodle API error: {}", body);
                return null;
            }
            return node;
        } catch (Exception e) {
            log.error("Moodle API call failed: {}", e.getMessage());
            return null;
        }
    }
}
