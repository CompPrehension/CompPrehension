package org.vstu.compprehension.integration.moodle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;


@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MoodleLtiActivity {
    /**
     * instance id активности mod_lti (строка таблицы {@code lti}).
     * Это НЕ идентификатор колонки журнала — для адресации журнала нужен {@link #courseModuleId}.
     */
    @JsonProperty("id")
    private Long id;
    /**
     * Course module id (cmid) — идентификатор, который ожидает
     * {@code core_grades_update_grades} в параметре {@code activityid}.
     */
    @JsonProperty("coursemodule")
    private Long courseModuleId;
    private UriComponents toolUrl;
    private final Map<String, String> customParameters = new LinkedHashMap<>();
    @JsonProperty("grade")
    private Double gradeMax;

    @JsonProperty("toolurl")
    public void setToolUrl(String toolUrl) {
        this.toolUrl = (toolUrl == null || toolUrl.isBlank()) ? null : UriComponentsBuilder.fromUriString(toolUrl).build();
    }

    @JsonProperty("instructorcustomparameters")
    public void setCustomParameters(String raw) {
        customParameters.clear();
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String line : raw.split("\\R")) {
            int eq = line.indexOf('=');
            if (eq > 0) {
                customParameters.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }
    }
}
