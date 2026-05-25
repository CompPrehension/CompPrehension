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
import java.util.Collections;
import java.util.List;

/**
 * Клиент Moodle WS REST API. На текущий момент поддерживает один метод:
 * {@code core_enrol_get_enrolled_users_with_capability} — bulk-запрос
 * пользователей, имеющих заданные capabilities в указанных курсах.
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
            return parseResponse(raw);
        } catch (Exception ex) {
            log.error("Moodle WS request failed: baseUrl={}", baseUrl, ex);
            return Collections.emptyList();
        }
    }

    private List<MoodleCapabilityResult> parseResponse(String raw) throws Exception {
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
        return objectMapper.convertValue(root, new TypeReference<List<MoodleCapabilityResult>>() {});
    }
}
