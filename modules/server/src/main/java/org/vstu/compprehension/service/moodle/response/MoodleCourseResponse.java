package org.vstu.compprehension.service.moodle.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ответ core_enrol_get_users_courses — элемент массива.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MoodleCourseResponse {
    @JsonProperty("id")
    private long id;
    @JsonProperty("shortname")
    private String shortname;
    @JsonProperty("fullname")
    private String fullname;
}
