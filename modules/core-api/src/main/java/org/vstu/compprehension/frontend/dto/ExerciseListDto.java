package org.vstu.compprehension.frontend.dto;

import java.util.List;

public record ExerciseListDto(List<ExerciseDto> exercises, ExerciseListPermissionsDto permissions) {
}
