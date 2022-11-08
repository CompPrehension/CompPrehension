package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Value
@Builder
public class ExerciseCardDto {
    @NotNull Long id;
    @NotNull String name;
    @NotNull String domainId;
    @NotNull String strategyId;
    @NotNull String backendId;
    float complexity;
    int numberOfQuestions;
    @NotNull List<ExerciseLawDto> laws;
    @NotNull List<ExerciseLConceptDto> concepts;

    //@NotNull List<String> tags;
    /*
    @NotNull ExerciseOptionsEntity options;
    @NotNull List<ExerciseLawsEntity> laws;
    @NotNull List<ExerciseConceptEntity> concepts;
    */
}

