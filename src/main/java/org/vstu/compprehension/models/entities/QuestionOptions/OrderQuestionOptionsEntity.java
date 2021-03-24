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
@SuperBuilder @Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderQuestionOptionsEntity extends QuestionOptionsEntity {
    /// Show answer trace
    private boolean showTrace = false;

    /// Same answers can be selected several times
    private boolean multipleSelectionEnabled = true;

    /// All answers must be selected
    private boolean requireAllAnswers = true;

    /// Order number options
    private OrderNumberOptions orderNumberOptions = new OrderNumberOptions();

    @Data @NoArgsConstructor @AllArgsConstructor
    @Builder @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderNumberOptions {
        private String delimiter = "/";
        private OrderNumberPosition position = OrderNumberPosition.SUFFIX;
        private String[] replacers;
    }

    @Jacksonized
    public enum OrderNumberPosition {
        @JsonProperty("PREFIX")
        PREFIX,
        @JsonProperty("SUFFIX")
        SUFFIX,
        @JsonProperty("NONE")
        NONE,
    }
}



