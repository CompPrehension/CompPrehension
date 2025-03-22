package org.vstu.compprehension.models.entities.QuestionOptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@SuperBuilder @Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchingQuestionOptionsEntity extends QuestionOptionsEntity {
    /// Preferred display mode
    private DisplayMode displayMode;

    /// Same answers can be selected several times
    private boolean multipleSelectionEnabled;

    public enum DisplayMode {
        @JsonProperty("combobox")
        COMBOBOX,
        @JsonProperty("dragNdrop")
        DRAGNDROP,
    }
}
