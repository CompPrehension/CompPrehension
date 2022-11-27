package org.vstu.compprehension.models.entities.exercise;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.dto.ExerciseConceptDto;
import org.vstu.compprehension.dto.ExerciseLawDto;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Value
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExerciseStageEntity {
    @Builder.Default
    private int numberOfQuestions = 5;
    @Builder.Default
    private @NotNull List<ExerciseLawDto> laws = new ArrayList<>(0);
    @Builder.Default
    private @NotNull List<ExerciseConceptDto> concepts = new ArrayList<>(0);
}
