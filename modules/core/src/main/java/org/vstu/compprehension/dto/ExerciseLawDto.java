package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Value;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;

@Value @Builder
public class ExerciseLawDto {
    String name;
    RoleInExercise kind;
}
