package org.vstu.compprehension.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@AllArgsConstructor @NoArgsConstructor
@SuperBuilder
@Jacksonized @JsonIgnoreProperties(ignoreUnknown = true)
public class ExerciseOptionsEntity {
    private Boolean newQuestionGenerationEnabled;
    private Boolean supplementaryQuestionsEnabled;
    private Boolean correctAnswerGenerationEnabled;
}
