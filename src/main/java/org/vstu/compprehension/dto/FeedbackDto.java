package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackDto {
    private Float grade;
    private Integer totalSteps;
    private Integer stepsLeft;
    private Integer stepsWithErrors;
    private String[] errors;
}

