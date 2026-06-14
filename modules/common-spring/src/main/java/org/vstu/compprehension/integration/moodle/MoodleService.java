package org.vstu.compprehension.integration.moodle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Клиент Moodle WS REST API. Один публичный метод = одна WS-функция:
 *
 * <p>URL и токен передаются явно (не из конфига) — конфиг живёт
 * в {@code WsFuncMoodleConfig}, lookup делает caller.
 */
@Service
@Log4j2
public class MoodleService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Bulk-запрос: для каждого {@link CourseCapabilityRequest#externalCourseId} проверяет
     * перечисленные {@link CourseCapabilityRequest#capabilities} и возвращает список юзеров.
     *
     * <p>Возвращает плоский список {@link MoodleCapabilityResult} по парам (courseId, capabilityName).
     */
    public List<MoodleCapabilityResult> getUsersWithCapabilityBulk(
            String baseUrl, String wsToken, List<CourseCapabilityRequest> requests) {

        if (requests == null || requests.isEmpty()) return Collections.emptyList();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        for (int i = 0; i < requests.size(); i++) {
            var r = requests.get(i);
            body.add(String.format("coursecapabilities[%d][courseid]", i), r.externalCourseId());
            int j = 0;
            for (var cap : r.capabilities()) {
                body.add(String.format("coursecapabilities[%d][capabilities][%d]", i, j++), cap);
            }
        }
        body.add("options[0][name]", "userfields");
        body.add("options[0][value]", "id");

        URI uri = URI.create("""
                %s/webservice/rest/server.php\
                ?wstoken=%s\
                &wsfunction=core_enrol_get_enrolled_users_with_capability\
                &moodlewsrestformat=json"""
                .formatted(baseUrl, wsToken));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            String raw = restTemplate.postForObject(uri, entity, String.class);
            return parseGetUsersWithCapabilityBulkResponse(raw);
        } catch (Exception ex) {
            log.error("Moodle WS request failed: baseUrl={}", baseUrl, ex);
            return Collections.emptyList();
        }
    }

    /**
     * Возвращает подмножество {@code courseIds}, реально существующих в Moodle, через
     * {@code core_course_get_courses(options[ids][])}. Эта функция итерирует только по
     * НАЙДЕННЫМ записям курсов, поэтому отсутствующие id просто не попадают в ответ — без
     * исключения. Удалённые курсы вычисляются как разность {@code courseIds − результат}
     * за один запрос.
     *
     * <p>Намеренно НЕ используется {@code core_course_get_courses_by_field(field=ids)}:
     * та функция итерирует по ЗАПРОШЕННЫМ id и роняет весь вызов PHP-фаталом
     * ({@code "Attempt to assign property \"contextvalidated\" on null"}) на первом же
     * отсутствующем курсе — т.е. не годится для обнаружения удалённых курсов.
     *
     * @throws MoodleWsException если вызов не удался (транспорт, нераспарсиваемый ответ
     *                           или объект-исключение Moodle) — в этом случае caller НЕ должен делать
     *                           выводов об удалении курсов.
     */
    public Set<String> findExistingCourseIds(String baseUrl, String wsToken, Collection<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) return Collections.emptySet();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        int i = 0;
        for (String courseId : courseIds) {
            body.add(String.format("options[ids][%d]", i++), courseId);
        }

        URI uri = URI.create("""
                %s/webservice/rest/server.php\
                ?wstoken=%s\
                &wsfunction=core_course_get_courses\
                &moodlewsrestformat=json"""
                .formatted(baseUrl, wsToken));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        String raw;
        try {
            raw = restTemplate.postForObject(uri, entity, String.class);
        } catch (Exception ex) {
            throw new MoodleWsException("core_course_get_courses transport failure: baseUrl=" + baseUrl, ex);
        }
        return parseExistingCourseIds(raw);
    }

    private Set<String> parseExistingCourseIds(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new MoodleWsException("core_course_get_courses returned empty response");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception ex) {
            throw new MoodleWsException("cannot parse core_course_get_courses response: " + raw, ex);
        }
        if (root.isObject() && root.has("exception")) {
            throw new MoodleWsException("core_course_get_courses returned exception: " + raw);
        }
        if (!root.isArray()) {
            throw new MoodleWsException("core_course_get_courses unexpected response (not an array): " + raw);
        }
        Set<String> ids = new HashSet<>();
        for (JsonNode course : root) {
            JsonNode id = course.path("id");
            if (!id.isMissingNode()) {
                ids.add(id.asText());
            }
        }
        return ids;
    }

    private List<MoodleCapabilityResult> parseGetUsersWithCapabilityBulkResponse(String raw) throws Exception {
        if (raw == null || raw.isBlank()) return Collections.emptyList();

        JsonNode root = objectMapper.readTree(raw);
        if (root.isObject() && root.has("exception")) {
            log.warn("Moodle WS returned exception: {}", raw);
            return Collections.emptyList();
        }
        if (!root.isArray()) {
            log.warn("Moodle WS unexpected response (not an array): {}", raw);
            return Collections.emptyList();
        }
        return objectMapper.convertValue(root, new TypeReference<List<MoodleCapabilityResult>>() {
        });
    }

    /**
     * LTI-активности курса через {@code mod_lti_get_ltis_by_courses}. Ответ — объект
     * {@code { "ltis": [...], "warnings": [...] }}; берём узел {@code ltis}.
     *
     * <p>Используется для обнаружения активности {@code mod_lti}, соответствующей упражнению,
     * чтобы адресовать оценку в нужную колонку журнала. Ошибка → пустой список + лог.
     */
    public List<MoodleLtiActivity> getLtiActivitiesInCourse(String baseUrl, String wsToken, String moodleCourseId) {
        if (moodleCourseId == null || moodleCourseId.isBlank()) return Collections.emptyList();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("courseids[0]", moodleCourseId);

        URI uri = URI.create("""
                %s/webservice/rest/server.php\
                ?wstoken=%s\
                &wsfunction=mod_lti_get_ltis_by_courses\
                &moodlewsrestformat=json"""
                .formatted(baseUrl, wsToken));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            String raw = restTemplate.postForObject(uri, entity, String.class);
            return parseLtiActivities(raw);
        } catch (Exception ex) {
            log.error("Moodle WS mod_lti_get_ltis_by_courses failed: baseUrl={}, courseId={}",
                    baseUrl, moodleCourseId, ex);
            return Collections.emptyList();
        }
    }

    private List<MoodleLtiActivity> parseLtiActivities(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();

        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception ex) {
            log.warn("cannot parse mod_lti_get_ltis_by_courses response: {}", raw, ex);
            return Collections.emptyList();
        }
        if (root.isObject() && root.has("exception")) {
            log.warn("mod_lti_get_ltis_by_courses returned exception: {}", raw);
            return Collections.emptyList();
        }
        // Ответ - объект { ltis: [...] }; на всякий случай поддерживаем и голый массив.
        JsonNode ltis = root.isArray() ? root : root.path("ltis");
        if (!ltis.isArray()) {
            log.warn("mod_lti_get_ltis_by_courses unexpected response (no ltis array): {}", raw);
            return Collections.emptyList();
        }
        return objectMapper.convertValue(ltis, new TypeReference<List<MoodleLtiActivity>>() {
        });
    }

    /**
     * Идентификатор источника оценки для {@code core_grades_update_grades}.
     */
    private static final String GRADE_SOURCE = "СompPrehension";

    /**
     * Записывает {@link MoodleGrade} в колонку журнала активности {@code mod_lti}
     * ({@code courseId} + {@code courseModuleId}) для студента {@code studentId}
     * через {@code core_grades_update_grades}.
     *
     * <p>Параметр {@code core_grades_update_grades.activityid} ожидает именно course module id
     * (cmid), а не instance id строки {@code lti} — Moodle резолвит его через
     * {@code get_coursemodule_from_id(itemmodule, activityid)}.
     *
     * @return {@code true}, если Moodle вернул код {@code 0} (GRADE_UPDATE_OK) без исключения.
     */
    public boolean updateGradeInCourse(
            String baseUrl,
            String wsToken,
            String externalCourseId,
            long courseModuleId,
            String studentId,
            MoodleGrade grade
    ) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("source", GRADE_SOURCE);
        body.add("courseid", externalCourseId);
        body.add("component", "mod_lti");
        body.add("activityid", String.valueOf(courseModuleId));
        body.add("itemnumber", "0");
        body.add("grades[0][studentid]", studentId);
        body.add("grades[0][grade]", String.valueOf(grade.rawScore()));

        URI uri = URI.create("""
                %s/webservice/rest/server.php\
                ?wstoken=%s\
                &wsfunction=core_grades_update_grades\
                &moodlewsrestformat=json"""
                .formatted(baseUrl, wsToken));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        String raw;
        try {
            raw = restTemplate.postForObject(uri, entity, String.class);
        } catch (Exception ex) {
            log.error("Moodle WS core_grades_update_grades failed: baseUrl={}, courseId={}, cmid={}",
                    baseUrl, externalCourseId, courseModuleId, ex);
            return false;
        }
        if (raw == null || raw.isBlank()) {
            log.warn("core_grades_update_grades returned empty response");
            return false;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception ex) {
            log.warn("cannot parse core_grades_update_grades response: {}", raw, ex);
            return false;
        }
        if (root.isObject() && root.has("exception")) {
            log.warn("core_grades_update_grades returned exception: {}", raw);
            return false;
        }
        // GRADE_UPDATE_OK == 0
        boolean ok = root.isNumber() && root.asInt() == 0;
        if (!ok) {
            log.warn("core_grades_update_grades returned non-OK code: {}", raw);
        }
        return ok;
    }
}
