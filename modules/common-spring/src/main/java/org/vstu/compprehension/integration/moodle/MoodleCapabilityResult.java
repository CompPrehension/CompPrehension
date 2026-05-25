package org.vstu.compprehension.integration.moodle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoodleCapabilityResult(
        @JsonProperty("courseid")   String courseId,
        @JsonProperty("capability") String capabilityName,
        @JsonProperty("users")      List<MoodleUserRef> courseMembers
) {
}
