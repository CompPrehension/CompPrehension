package org.vstu.compprehension.models.entities.QuestionOptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class MultiChoiceOptionsEntity extends QuestionOptionsEntity {
    @Builder.Default
    private DisplayMode displayMode = DisplayMode.SWITCH;

    private String[] selectorReplacers;

    public enum DisplayMode {
        @JsonProperty("switch")
        SWITCH,
        @JsonProperty("dragNdrop")
        DRAGNDROP,
    }
}
