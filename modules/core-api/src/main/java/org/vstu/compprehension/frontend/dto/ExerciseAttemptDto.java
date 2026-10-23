package org.vstu.compprehension.frontend.dto;

import lombok.Builder;
import lombok.Data;
import org.vstu.compprehension.enums.AttemptStatus;

@Data
@Builder
public class ExerciseAttemptDto {
    private Long userId;
    private Long attemptId;
    private Long exerciseId;
    private Long courseId;
    private AttemptStatus status;
    private Long[] questionIds;
}
