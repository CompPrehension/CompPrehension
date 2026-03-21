package org.vstu.compprehension.service.moodle.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Обёртка для ответа mod_lti_get_ltis_by_courses: {@code {"ltis":[...], "warnings":[]}}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MoodleLtiListResponse {
    @JsonProperty("ltis")
    private List<MoodleLtiActivityResponse> ltis;
}
