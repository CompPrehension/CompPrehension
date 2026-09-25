package org.vstu.compprehension.data.questionoptions;

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
public class MultiChoiceOptionsData extends QuestionOptionsData {
    public static final int SWITCH_OFF = 0;
    public static final int SWITCH_ON = 1;
    public static final int DRAGNDROP_SELECTED_GROUP = 0;

    @Builder.Default
    private DisplayMode displayMode = DisplayMode.SWITCH;

    private String[] selectorReplacers;

    public int selectedValue() {
        return switch (displayMode) {
            case SWITCH -> SWITCH_ON;
            case DRAGNDROP -> DRAGNDROP_SELECTED_GROUP;
        };
    }

    public boolean isSelected(int value) {
        return value == selectedValue();
    }

    public enum DisplayMode {
        @JsonProperty("switch")
        SWITCH,
        @JsonProperty("dragNdrop")
        DRAGNDROP,
    }
}
