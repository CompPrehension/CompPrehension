package org.vstu.compprehension.dto;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class ExerciseDto {
    @NotNull Long id;
    @NotNull String name;
}
