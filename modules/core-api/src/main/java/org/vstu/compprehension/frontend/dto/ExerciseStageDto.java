package org.vstu.compprehension.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseStageDto {
    private @NotNull String strategyId;
    private @NotNull Map<String, Object> strategySettings;
    private int numberOfQuestions;
    private float complexity;
    private @NotNull List<ExerciseLawDto> laws;
    private @NotNull List<ExerciseConceptDto> concepts;
    private @NotNull List<ExerciseSkillDto> skills;
}
