package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackDto {
    private Float grade;
    private Integer correctSteps;
    private Integer stepsLeft;
    private Integer stepsWithErrors;
    private String[] errors;
    private Long[] violations;
    private String explanation;
    private Long[][] correctAnswers;
}

