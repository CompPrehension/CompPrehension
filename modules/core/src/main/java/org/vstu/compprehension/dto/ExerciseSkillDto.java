package org.vstu.compprehension.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.data.enums.RoleInExercise;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSkillDto {
    private String name;
    private RoleInExercise kind;
}
