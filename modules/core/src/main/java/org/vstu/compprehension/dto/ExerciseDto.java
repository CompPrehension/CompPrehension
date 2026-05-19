package org.vstu.compprehension.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Value
public class ExerciseDto {
    @NotNull Long id;
    @NotNull String name;
    @JsonProperty("isPublic") boolean isPublic;
    UUID guid;
}
