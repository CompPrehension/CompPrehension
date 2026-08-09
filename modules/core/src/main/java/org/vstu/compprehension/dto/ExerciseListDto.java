package org.vstu.compprehension.dto;

import java.util.List;

public record ExerciseListDto(List<ExerciseDto> exercises, List<String> permissions) {
}
