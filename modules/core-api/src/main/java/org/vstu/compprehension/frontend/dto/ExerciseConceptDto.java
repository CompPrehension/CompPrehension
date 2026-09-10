package org.vstu.compprehension.frontend.dto;

import lombok.*;
import org.vstu.compprehension.enums.RoleInExercise;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseConceptDto {
    private String name;
    private RoleInExercise kind;
}
