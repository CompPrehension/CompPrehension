package org.vstu.compprehension.integration.moodle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoodleUserRef(Long id) {
}
