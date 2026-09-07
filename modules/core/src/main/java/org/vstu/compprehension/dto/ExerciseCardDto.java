package org.vstu.compprehension.dto;

import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
    private @NotNull ExerciseOptionsData options;
    @JsonProperty("isPublic") private boolean isPublic;

    @JsonProperty(access = Access.READ_ONLY)
    private ExerciseCardPermissionsDto permissions;

    //@NotNull List<String> tags;
    /*
    @NotNull ExerciseOptionsData options;
    @NotNull List<ExerciseLawsEntity> laws;
    @NotNull List<ExerciseConceptEntity> concepts;
    */
}
