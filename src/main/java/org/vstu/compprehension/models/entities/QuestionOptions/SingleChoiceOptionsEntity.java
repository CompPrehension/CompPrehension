package org.vstu.compprehension.models.entities.QuestionOptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@AllArgsConstructor @NoArgsConstructor
@SuperBuilder
@Jacksonized @JsonIgnoreProperties(ignoreUnknown = true)
public class SingleChoiceOptionsEntity extends QuestionOptionsEntity {
    @Builder.Default
    private DisplayMode displayMode = DisplayMode.RADIO;

    public enum DisplayMode {
        @JsonProperty("radio")
        RADIO,
        @JsonProperty("dragNdrop")
        DRAGNDROP,
    }
}
