package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackDto {
    private float grade;
    private Integer totalSteps;
    private int stepsLeft;
    private String[] errors;
}

