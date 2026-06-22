package org.vstu.compprehension.moodle.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoodleUserRef(Long id) {
}
