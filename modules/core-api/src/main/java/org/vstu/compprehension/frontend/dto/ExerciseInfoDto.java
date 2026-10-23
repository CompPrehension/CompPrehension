package org.vstu.compprehension.frontend.dto;

import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class ExerciseInfoDto {
    private Long id;
    private ExerciseOptionsData options;
}
