package org.vstu.compprehension.service.moodle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.vstu.compprehension.config.MoodleWsRegistrationsProperties.Registration;
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
 * HTTP-клиент для Moodle Web Services REST API. Stateless: каждый метод принимает
 * {@link Registration} (base URL + admin token); одна инсталляция MoodleService работает
 * с произвольным числом Moodle-инстансов через {@code MoodleWsRegistrationsProperties}.
 *
 * <p>Бин создаётся только при {@code compprehension.grade-passback.moodle-ws.enabled=true}.
 *
 * <p>TECH DEBT: wstoken - долгоживущий admin-токен. В продакшне заменить на сервис-аккаунт
 * с capabilities: core_user_get_users_by_field, core_enrol_get_users_courses,
 * mod_lti_get_ltis_by_courses, core_grades_update_grades.
 */
@Service
@ConditionalOnProperty(
        prefix = "compprehension.grade-passback.moodle-ws",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@Log4j2
public class MoodleService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MoodleService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Ищет пользователя Moodle по email (core_user_get_users_by_field).
     */
    public Optional<MoodleUserResponse> findUserByEmail(Registration reg, String email) {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("field", "email");
        params.add("values[0]", email);
        return executeRequest(reg, GET_USERS_BY_FIELD, params, MoodleUserResponse[].class)
                .filter(arr -> arr.length > 0)
                .map(arr -> arr[0]);
    }

    /**
     * Возвращает курсы, в которых зачислен пользователь (core_enrol_get_users_courses).
     */
    public List<MoodleCourseResponse> getEnrolledCourses(Registration reg, long moodleUserId) {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("userid", String.valueOf(moodleUserId));
        return executeRequest(reg, GET_USERS_COURSES, params, MoodleCourseResponse[].class)
                .map(Arrays::asList)
                .orElse(List.of());
    }

    /**
     * Возвращает LTI-активности указанных курсов (mod_lti_get_ltis_by_courses).
     */
    public List<MoodleLtiActivityResponse> getLtiActivities(Registration reg, List<Long> courseIds) {
        if (courseIds.isEmpty()) return List.of();
        var request = GetLtiActivitiesRequest.builder().courseIds(courseIds).build();
        return executeRequest(reg, GET_LTIS_BY_COURSES, request, MoodleLtiListResponse.class)
                .map(r -> r.getLtis() != null ? r.getLtis() : List.<MoodleLtiActivityResponse>of())
                .orElse(List.of());
    }

    /**
     * Выставляет оценку через core_grades_update_grades.
     */
    public void updateGrade(Registration reg, UpdateGradeRequest request) {
        MultiValueMap<String, String> params = toFormBody(request);
        // source — метка в mdl_grade_grades_history.source: оценка выставлена CompPrehension.
        params.add("source", "compph");
        // component — тип активности-владельца grade item; для LTI-активности это mod_lti.
        params.add("component", "mod_lti");
        // itemnumber — порядковый номер grade item внутри компонента (у LTI один item → 0).
        params.add("itemnumber", "0");
        executeRequest(reg, UPDATE_GRADES, params, Integer.class);
        log.debug("core_grades_update_grades: courseId={}, courseModuleId={}",
                request.getCourseId(), request.getCourseModuleId());
    }

    private <T> Optional<T> executeRequest(Registration reg, MoodleWebServiceFunction fn,
                                           MoodleRequest request, Class<T> responseType) {
        MultiValueMap<String, String> body = buildBaseBody(reg, fn);
        body.addAll(toFormBody(request));
        return executePostRequest(reg, body, fn, responseType);
    }

    private <T> Optional<T> executeRequest(Registration reg, MoodleWebServiceFunction fn,
                                           MultiValueMap<String, String> params, Class<T> responseType) {
        MultiValueMap<String, String> body = buildBaseBody(reg, fn);
        body.addAll(params);
        return executePostRequest(reg, body, fn, responseType);
    }

    private MultiValueMap<String, String> buildBaseBody(Registration reg, MoodleWebServiceFunction fn) {
        var body = new LinkedMultiValueMap<String, String>();
        body.add("wstoken", reg.getWebserviceToken());
        body.add("wsfunction", fn.getWsFunctionName());
        body.add("moodlewsrestformat", "json");
        return body;
    }

    private <T> Optional<T> executePostRequest(Registration reg, MultiValueMap<String, String> body,
                                               MoodleWebServiceFunction fn, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            var entity = new HttpEntity<>(body, headers);
            return Optional.ofNullable(
                    restTemplate.postForObject(reg.getBaseUrl() + "/webservice/rest/server.php",
                            entity, responseType));
        } catch (Exception e) {
            log.error("Moodle API call failed [{} @ {}]: {}", fn, reg.getBaseUrl(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Конвертирует MoodleRequest в MultiValueMap для form-encoded тела.
     * List-поля раскрываются как key[0]=val0, key[1]=val1, ...
     * Вложенные объекты раскрываются как key[i][subkey]=subval.
     */
    private MultiValueMap<String, String> toFormBody(MoodleRequest request) {
        Map<String, Object> map = objectMapper.convertValue(request, new TypeReference<Map<String, Object>>() {
        });
        MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
        map.forEach((k, v) -> flattenInto(result, k, v));
        return result;
    }

    @SuppressWarnings("unchecked")
    private void flattenInto(MultiValueMap<String, String> result, String prefix, Object value) {
        switch (value) {
            case null -> {
            }
            case List<?> list -> {
                for (int i = 0; i < list.size(); i++) {
                    flattenInto(result, prefix + "[" + i + "]", list.get(i));
                }
            }
            case Map<?, ?> map ->
                    ((Map<String, Object>) map).forEach((k, v) -> flattenInto(result, prefix + "[" + k + "]", v));
            default -> result.add(prefix, String.valueOf(value));
        }
    }
}
