package org.vstu.compprehension.frontend.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.enums.RoleInExercise;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSkillDto implements Serializable {
    private String name;
    private RoleInExercise kind;
}
