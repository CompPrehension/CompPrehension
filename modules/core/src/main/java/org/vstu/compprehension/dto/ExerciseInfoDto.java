package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.vstu.compprehension.models.entities.exercise.ExerciseOptionsEntity;

@Data
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class ExerciseInfoDto {
    private Long id;
    private ExerciseOptionsEntity options;
}
