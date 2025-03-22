package org.vstu.compprehension.models.entities.QuestionOptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter @Setter
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
