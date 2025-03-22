package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Data;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;

@Data
@Builder
public class ExerciseAttemptDto {
    private Long userId;
    private Long attemptId;
    private Long exerciseId;
    private AttemptStatus status;
    private Long[] questionIds;
}
