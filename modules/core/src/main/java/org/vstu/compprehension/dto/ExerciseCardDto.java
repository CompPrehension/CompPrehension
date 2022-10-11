package org.vstu.compprehension.dto;

import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.entities.ExerciseConceptEntity;
import org.vstu.compprehension.models.entities.ExerciseLawsEntity;
import org.vstu.compprehension.models.entities.ExerciseOptionsEntity;

import java.util.List;

@Value
public class ExerciseCardDto {
    @NotNull Long id;
    @NotNull String name;
    @NotNull String domainId;
    @NotNull String strategyId;
    @NotNull String backendId;
    //@NotNull List<String> tags;
    /*
    @NotNull ExerciseOptionsEntity options;
    @NotNull List<ExerciseLawsEntity> laws;
    @NotNull List<ExerciseConceptEntity> concepts;
    */
}
