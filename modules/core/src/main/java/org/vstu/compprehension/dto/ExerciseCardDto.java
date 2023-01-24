package org.vstu.compprehension.dto;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.entities.exercise.ExerciseOptionsEntity;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class ExerciseCardDto {
    private @NotNull Long id;
    private @NotNull String name;
    private @NotNull String domainId;
    private @NotNull String strategyId;
    private @NotNull String backendId;
    private float complexity;
    private @NotNull List<String> tags;
    private @NotNull List<ExerciseStageDto> stages;
    private @NotNull ExerciseOptionsEntity options;

    //@NotNull List<String> tags;
    /*
    @NotNull ExerciseOptionsEntity options;
    @NotNull List<ExerciseLawsEntity> laws;
    @NotNull List<ExerciseConceptEntity> concepts;
    */
}

