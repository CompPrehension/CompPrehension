package org.vstu.compprehension.data.exercise;

import java.io.Serializable;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.Nullable;

@Data
@AllArgsConstructor @NoArgsConstructor
@SuperBuilder
@Jacksonized @JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExerciseOptionsData implements Serializable {
    private ExerciseSurveyOptionsData surveyOptions;
    private boolean newQuestionGenerationEnabled;
    private boolean supplementaryQuestionsEnabled;
    private boolean correctAnswerGenerationEnabled;
    private boolean debugButtonEnabled;
    private boolean forceNewAttemptCreationEnabled;
    @Builder.Default
    private int maxExpectedConcurrentStudents = 10;
    private Integer generatorThreshold;
    private Integer generatorAdditionalQuestionsToGenerate;
    // Тип настроек знает только стратегия упражнения: здесь они хранятся как есть.
    @With
    private @Nullable Map<String, Object> strategySettings;

    @Data
    @AllArgsConstructor @NoArgsConstructor
    @SuperBuilder
    @Jacksonized @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExerciseSurveyOptionsData implements Serializable {
        private Boolean enabled;
        private String surveyId;
    }
}
