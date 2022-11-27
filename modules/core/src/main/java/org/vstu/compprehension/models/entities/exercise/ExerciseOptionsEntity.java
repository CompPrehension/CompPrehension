package org.vstu.compprehension.models.entities.exercise;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@AllArgsConstructor @NoArgsConstructor
@SuperBuilder
@Jacksonized @JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExerciseOptionsEntity {
    private ExerciseSurveyOptionsEntity surveyOptions;
    private Boolean newQuestionGenerationEnabled;
    private Boolean supplementaryQuestionsEnabled;
    private Boolean correctAnswerGenerationEnabled;
    private Boolean forceNewAttemptCreationEnabled;

    @Data
    @AllArgsConstructor @NoArgsConstructor
    @SuperBuilder
    @Jacksonized @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExerciseSurveyOptionsEntity {
        private Boolean enabled;
        private String surveyId;
    }
}
