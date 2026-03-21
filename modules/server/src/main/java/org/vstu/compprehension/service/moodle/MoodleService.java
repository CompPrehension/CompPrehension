package org.vstu.compprehension.service.moodle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.vstu.compprehension.service.moodle.request.GetLtiActivitiesRequest;
import org.vstu.compprehension.service.moodle.request.MoodleRequest;
import org.vstu.compprehension.service.moodle.request.UpdateGradeRequest;
import org.vstu.compprehension.service.moodle.response.MoodleCourseResponse;
import org.vstu.compprehension.service.moodle.response.MoodleLtiActivityResponse;
import org.vstu.compprehension.service.moodle.response.MoodleLtiListResponse;
import org.vstu.compprehension.service.moodle.response.MoodleUserResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.vstu.compprehension.service.moodle.MoodleWebServiceFunction.*;

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
public class MoodleService {

    @Value("${moodle.base-url:}")
    private String moodleBaseUrl;

    @Value("${moodle.webservice-token:}")
    private String wsToken;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MoodleService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Возвращает true если интеграция с Moodle Web Services настроена.
     */
    public boolean isConfigured() {
        // временно, заменить на ConditionOnProperty
        return !moodleBaseUrl.isBlank() && !wsToken.isBlank();
    }

    /**
     * Ищет пользователя Moodle по email.
     * Вызывает core_user_get_users_by_field.
     */
    public Optional<MoodleUserResponse> findUserByEmail(String email) {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("field", "email");
        params.add("values[0]", email);
        return executeRequest(GET_USERS_BY_FIELD, params, MoodleUserResponse[].class)
                .filter(arr -> arr.length > 0)
                .map(arr -> arr[0]);
    }

    /**
     * Возвращает список курсов, в которых зачислен пользователь.
     * Вызывает core_enrol_get_users_courses.
     */
    public List<MoodleCourseResponse> getEnrolledCourses(long moodleUserId) {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("userid", String.valueOf(moodleUserId));
        return executeRequest(GET_USERS_COURSES, params, MoodleCourseResponse[].class)
                .map(Arrays::asList)
                .orElse(List.of());
    }

    /**
     * Возвращает LTI-активности для указанных курсов.
     * Вызывает mod_lti_get_ltis_by_courses.
     */
    public List<MoodleLtiActivityResponse> getLtiActivities(List<Long> courseIds) {
        if (courseIds.isEmpty()) return List.of();
        var request = GetLtiActivitiesRequest.builder().courseIds(courseIds).build();
        return executeRequest(GET_LTIS_BY_COURSES, request, MoodleLtiListResponse.class)
                .map(r -> r.getLtis() != null ? r.getLtis() : List.<MoodleLtiActivityResponse>of())
                .orElse(List.of());
    }

    /**
     * Выставляет оценку через core_grades_update_grades.
     *
     * @param request объект с courseId, courseModuleId и списком grades
     */
    public void updateGrade(UpdateGradeRequest request) {
        MultiValueMap<String, String> params = toFormBody(request);
        // source - метка в истории оценок (mdl_grade_grades_history.source),
        //          показывает, что оценка выставлена CompPrehension, а не вручную.
        params.add("source", "compph");
        // component - тип активности-владельца grade item; для LTI-активности это mod_lti.
        params.add("component", "mod_lti");
        // itemnumber - порядковый номер grade item внутри компонента (нумерация с 0).
        //              У LTI-активности всегда один grade item, поэтому 0.
        params.add("itemnumber", "0");
        executeRequest(UPDATE_GRADES, params, Integer.class);
        log.debug("core_grades_update_grades: courseId={}, courseModuleId={}",
                request.getCourseId(), request.getCourseModuleId());
    }

    // ---- private: executeRequest overloads ----

    /** Для MoodleRequest-объектов — params строятся через toFormBody. */
    private <T> Optional<T> executeRequest(MoodleWebServiceFunction fn,
                                            MoodleRequest request,
                                            Class<T> responseType) {
        MultiValueMap<String, String> body = buildBaseBody(fn);
        body.addAll(toFormBody(request));
        return executePostRequest(body, fn, responseType);
    }

    /** Для inline-параметров (findUserByEmail, getEnrolledCourses, updateGrade с константами). */
    private <T> Optional<T> executeRequest(MoodleWebServiceFunction fn,
                                            MultiValueMap<String, String> params,
                                            Class<T> responseType) {
        MultiValueMap<String, String> body = buildBaseBody(fn);
        body.addAll(params);
        return executePostRequest(body, fn, responseType);
    }

    private MultiValueMap<String, String> buildBaseBody(MoodleWebServiceFunction fn) {
        var body = new LinkedMultiValueMap<String, String>();
        body.add("wstoken", wsToken);
        body.add("wsfunction", fn.getWsFunctionName());
        body.add("moodlewsrestformat", "json");
        return body;
    }

    private <T> Optional<T> executePostRequest(MultiValueMap<String, String> body,
                                               MoodleWebServiceFunction fn,
                                               Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            var entity = new HttpEntity<>(body, headers);
            return Optional.ofNullable(
                    restTemplate.postForObject(moodleBaseUrl + "/webservice/rest/server.php",
                            entity, responseType));
        } catch (Exception e) {
            log.error("Moodle API call failed [{}]: {}", fn, e.getMessage());
            return Optional.empty();
        }
    }

    // ---- private: form encoding ----

    /**
     * Конвертирует MoodleRequest в MultiValueMap для form-encoded тела.
     * List-поля раскрываются как key[0]=val0, key[1]=val1, ...
     * Вложенные объекты раскрываются как key[i][subkey]=subval.
     */
    private MultiValueMap<String, String> toFormBody(MoodleRequest request) {
        Map<String, Object> map = objectMapper.convertValue(request, new TypeReference<Map<String, Object>>() {});
        MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
        map.forEach((k, v) -> flattenInto(result, k, v));
        return result;
    }

    @SuppressWarnings("unchecked")
    private void flattenInto(MultiValueMap<String, String> result, String prefix, Object value) {
        if (value == null) return;
        if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                flattenInto(result, prefix + "[" + i + "]", list.get(i));
            }
        } else if (value instanceof Map<?, ?> map) {
            ((Map<String, Object>) map).forEach((k, v) -> flattenInto(result, prefix + "[" + k + "]", v));
        } else {
            result.add(prefix, String.valueOf(value));
        }
    }
}
