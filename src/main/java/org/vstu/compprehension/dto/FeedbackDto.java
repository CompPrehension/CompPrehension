package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackDto {
    private float grade;
    private int iterationsLeft;
    private int correctOptionsCount;
    private String[] errors;
}

