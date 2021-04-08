package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExerciseAttemptDto {
    private Long attemptId;
    private Long exerciseId;
    private Long[] questionIds;
}
