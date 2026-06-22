package org.vstu.compprehension.moodle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.vstu.compprehension.moodle.request.CourseCapabilityRequest;
import org.vstu.compprehension.moodle.request.MoodleGrade;
import org.vstu.compprehension.moodle.response.MoodleCapabilityResult;
import org.vstu.compprehension.moodle.response.MoodleLtiActivity;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Клиент Moodle WS REST API. Один публичный метод = одна WS-функция:
 */
@Service
public class MoodleService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Bulk-запрос: для каждого {@link CourseCapabilityRequest#externalCourseId} проверяет
     * перечисленные {@link CourseCapabilityRequest#capabilities} и возвращает список юзеров.
     *
     * <p>Возвращает плоский список {@link MoodleCapabilityResult} по парам (courseId, capabilityName).
     */
    public MoodleWsResult<List<MoodleCapabilityResult>> getUsersWithCapabilityBulk(
            String baseUrl, String wsToken, List<CourseCapabilityRequest> requests) {

        if (requests == null || requests.isEmpty()) {
            return new MoodleWsResult.Success<>(Collections.emptyList());
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        for (int i = 0; i < requests.size(); i++) {
            CourseCapabilityRequest r = requests.get(i);
            body.add(String.format("coursecapabilities[%d][courseid]", i), r.externalCourseId());
            int j = 0;
            for (String cap : r.capabilities()) {
                body.add(String.format("coursecapabilities[%d][capabilities][%d]", i, j++), cap);
            }
        }
        body.add("options[0][name]", "userfields");
        body.add("options[0][value]", "id");

        return execute(baseUrl, wsToken, "core_enrol_get_enrolled_users_with_capability", body,
                new TypeReference<List<MoodleCapabilityResult>>() {
                });
    }

    /**
     * Возвращает подмножество {@code courseIds}, реально существующих в Moodle, через
     * {@code core_course_get_courses(options[ids][])}. Эта функция итерирует только по
     * НАЙДЕННЫМ записям курсов, поэтому отсутствующие id просто не попадают в ответ — без
     * исключения. Удалённые курсы вычисляются как разность {@code courseIds − результат}
     * за один запрос.
     */
    public MoodleWsResult<Set<String>> findExistingCourseIds(String baseUrl, String wsToken, Collection<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return new MoodleWsResult.Success<>(Collections.emptySet());
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        int i = 0;
        for (String courseId : courseIds) {
            body.add(String.format("options[ids][%d]", i++), courseId);
        }

        return execute(baseUrl, wsToken, "core_course_get_courses", body,
                root -> {
                    if (!root.isArray()) {
                        throw new IllegalStateException("not an array");
                    }
                    Set<String> ids = new HashSet<>();
                    for (JsonNode course : root) {
                        JsonNode id = course.path("id");
                        if (!id.isMissingNode()) {
                            ids.add(id.asText());
                        }
                    }
                    return ids;
                });
    }

    /**
     * LTI-активности курса через {@code mod_lti_get_ltis_by_courses}. Ответ — объект
     * {@code { "ltis": [...], "warnings": [...] }}; берём узел {@code ltis}.
     *
     * <p>Используется для обнаружения активности {@code mod_lti}, соответствующей упражнению
     */
    public MoodleWsResult<List<MoodleLtiActivity>> getLtiActivitiesInCourse(String baseUrl, String wsToken, String moodleCourseId) {
        if (moodleCourseId == null || moodleCourseId.isBlank()) {
            return new MoodleWsResult.Success<>(Collections.emptyList());
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("courseids[0]", moodleCourseId);

        return execute(baseUrl, wsToken, "mod_lti_get_ltis_by_courses", body,
                root -> {
                    // Ответ - объект { ltis: [...] }; на всякий случай поддерживаем и голый массив.
                    JsonNode ltis = root.isArray() ? root : root.path("ltis");
                    if (!ltis.isArray()) {
                        throw new IllegalStateException("no ltis array");
                    }
                    return objectMapper.convertValue(ltis, new TypeReference<List<MoodleLtiActivity>>() {
                    });
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
     * @return {@link MoodleWsResult.Success} с {@code true}, если Moodle вернул код {@code 0}
     * (GRADE_UPDATE_OK); {@code Success(false)} — на любой иной код
     */
    public MoodleWsResult<Boolean> updateGradeInCourse(
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

        // GRADE_UPDATE_OK == 0
        return execute(baseUrl, wsToken, "core_grades_update_grades", body,
                root -> root.isNumber() && root.asInt() == 0);
    }

    private <T> MoodleWsResult<T> execute(
            String baseUrl,
            String wsToken,
            String wsFunction,
            MultiValueMap<String, String> body,
            TypeReference<T> type
    ) {
        return execute(baseUrl, wsToken, wsFunction, body, root -> objectMapper.convertValue(root, type));
    }

    /**
     * Общий конвейер: запрос, проверка узла {@code exception}, извлечение payload через {@code extractor}.
     * Любой сбой (транспорт, пустой/неразобранный ответ, объект-ошибка Moodle, исключение extractor'а)
     * превращается в {@link MoodleWsResult.Failure}; решение, как его трактовать, остаётся за caller'ом.
     */
    private <T> MoodleWsResult<T> execute(
            String baseUrl,
            String wsToken,
            String wsFunction,
            MultiValueMap<String, String> body,
            Function<JsonNode, T> payloadExtractor
    ) {
        URI uri = buildUri(baseUrl, wsToken, wsFunction);
        String raw;
        try {
            raw = restTemplate.postForObject(uri, formEntity(body), String.class);
        } catch (Exception ex) {
            return MoodleWsResult.Failure.local("transport_error", String.format("%s baseUrl=%s", wsFunction, baseUrl), ex);
        }
        if (raw == null || raw.isBlank()) {
            return MoodleWsResult.Failure.local("empty_response", wsFunction, null);
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception ex) {
            return MoodleWsResult.Failure.local("parse_error", String.format("%s: %s", wsFunction, raw), ex);
        }
        if (root.isObject() && root.has("exception")) {
            return MoodleWsResult.Failure.fromMoodle(objectMapper, root);
        }
        try {
            return new MoodleWsResult.Success<>(payloadExtractor.apply(root));
        } catch (Exception ex) {
            return MoodleWsResult.Failure.local("unexpected_response", String.format("%s: %s", wsFunction, raw), ex);
        }
    }

    private static URI buildUri(String baseUrl, String wsToken, String wsFunction) {
        return URI.create("""
                %s/webservice/rest/server.php\
                ?wstoken=%s\
                &wsfunction=%s\
                &moodlewsrestformat=json"""
                .formatted(baseUrl, wsToken, wsFunction));
    }

    private static HttpEntity<MultiValueMap<String, String>> formEntity(MultiValueMap<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(body, headers);
    }
}
