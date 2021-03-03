package com.example.demo.models.entities.QuestionOptions;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Data
@AllArgsConstructor @NoArgsConstructor
@SuperBuilder
public class OrderQuestionOptionsEntity extends QuestionOptionsEntity {
    /// Show answer trace
    private boolean showTrace = false;

    /// Should be enabled multiple selection of same answers
    private boolean enableMultipleSelection = true;

    /// Order number options
    private OrderNumberOptions orderNumberOptions = new OrderNumberOptions();

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class OrderNumberOptions {
        private String delimiter = "/";
        private OrderNumberPosition position = OrderNumberPosition.SUFFIX;
        private String[] replacers;
    }

    public enum OrderNumberPosition {
        @JsonProperty("PREFIX")
        PREFIX,
        @JsonProperty("SUFFIX")
        SUFFIX,
        @JsonProperty("NONE")
        NONE,
    }
}



