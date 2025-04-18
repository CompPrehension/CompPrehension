package org.vstu.compprehension.models.entities.exercise;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.dto.ExerciseConceptDto;
import org.vstu.compprehension.dto.ExerciseLawDto;
import org.vstu.compprehension.dto.ExerciseSkillDto;

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
    int numberOfQuestions = 5;
    
    @Builder.Default
    float complexity = 0.5f;
    
    @Builder.Default
    @NotNull List<ExerciseLawDto> laws = new ArrayList<>(0);
    
    @Builder.Default
    @NotNull List<ExerciseConceptDto> concepts = new ArrayList<>(0);

    @Builder.Default
    @NotNull List<ExerciseSkillDto> skills = new ArrayList<>(0);
}
