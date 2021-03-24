package org.vstu.compprehension.models.entities.QuestionOptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@AllArgsConstructor @NoArgsConstructor
@SuperBuilder @Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchingQuestionOptionsEntity extends QuestionOptionsEntity {
    private DisplayMode displayMode;

    public enum DisplayMode {
        @JsonProperty("combobox")
        COMBOBOX,
        @JsonProperty("dragNdrop")
        DRAGNDROP,
    }
}
