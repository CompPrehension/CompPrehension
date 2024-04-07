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
public class QuestionBankSearchRequestDto {
    private @NotNull String domainId;
    private @NotNull Float complexity;
    private @NotNull List<String> tags;
    private @NotNull List<ExerciseLawDto> laws;
    private @NotNull List<ExerciseConceptDto> concepts;
}
