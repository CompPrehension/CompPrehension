package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseStageDto {
    private int numberOfQuestions;
    private @NotNull List<ExerciseLawDto> laws;
    private @NotNull List<ExerciseConceptDto> concepts;
}
