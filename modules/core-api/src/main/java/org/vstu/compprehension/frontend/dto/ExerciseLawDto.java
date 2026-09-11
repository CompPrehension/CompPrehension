package org.vstu.compprehension.frontend.dto;

import java.io.Serializable;
import lombok.*;
import org.vstu.compprehension.enums.RoleInExercise;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseLawDto implements Serializable {
    private String name;
    private RoleInExercise kind;
}
