package org.vstu.compprehension.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.entities.exercise.ExerciseOptionsEntity;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseCardDto {
    private @NotNull Long id;
    private @NotNull String name;
    private @NotNull String domainId;
    private @NotNull String strategyId;
    private @NotNull String backendId;
    private @NotNull List<String> tags;
    private @NotNull List<ExerciseStageDto> stages;
    private @NotNull ExerciseOptionsEntity options;
    @JsonProperty("isPublic") private boolean isPublic;
    private UUID guid;

    //@NotNull List<String> tags;
    /*
    @NotNull ExerciseOptionsEntity options;
    @NotNull List<ExerciseLawsEntity> laws;
    @NotNull List<ExerciseConceptEntity> concepts;
    */
}

