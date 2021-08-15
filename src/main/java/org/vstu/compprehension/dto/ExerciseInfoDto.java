package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.ExerciseOptionsEntity;

@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ExerciseInfoDto {
    private Long id;
    private ExerciseOptionsEntity options;
}
