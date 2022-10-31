package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

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
    //@NotNull List<String> tags;
    /*
    @NotNull ExerciseOptionsEntity options;
    @NotNull List<ExerciseLawsEntity> laws;
    @NotNull List<ExerciseConceptEntity> concepts;
    */
}
