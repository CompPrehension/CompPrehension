package org.vstu.compprehension.service.moodle.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LTI-активность из ответа mod_lti_get_ltis_by_courses.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MoodleLtiActivityResponse {
    @JsonProperty("coursemodule")
    private long cmid;
    @JsonProperty("course")
    private long courseId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("toolurl")
    private String toolUrl;
    /** Распарсенные instructorcustomparameters: key → value */
    @Setter(AccessLevel.NONE)
    private Map<String, String> customParams;
    @JsonProperty("instructorchoiceacceptgrades")
    private int acceptGrades;
    @JsonProperty("grade")
    private double grade;

    @JsonProperty("instructorcustomparameters")
    public void setCustomParams(String raw) {
        if (raw == null || raw.isBlank()) {
            this.customParams = Map.of();
            return;
        }
        this.customParams = Arrays.stream(raw.split("[;\n]"))
                .map(line -> line.split("=", 2))
                .filter(kv -> kv.length == 2)
                .collect(Collectors.toMap(
                        kv -> kv[0].trim(),
                        kv -> kv[1].trim(),
                        (a, b) -> a));
    }
}
