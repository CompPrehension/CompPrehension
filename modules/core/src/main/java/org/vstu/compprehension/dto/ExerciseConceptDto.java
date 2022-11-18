package org.vstu.compprehension.dto;

import lombok.*;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseConceptDto {
    private String name;
    private RoleInExercise kind;
}
