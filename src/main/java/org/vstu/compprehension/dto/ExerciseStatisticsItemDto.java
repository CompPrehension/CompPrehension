package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExerciseStatisticsItemDto {
    private Long attemptId;
    private Integer questionsCount;
    private Integer totalInteractionsCount;
    private Integer totalInteractionsWithErrorsCount;
    private double averageGrade;
}
