package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class SupplementaryQuestionRequestDto {
    private Long exerciseAttemptId;
    private Long questionId;
    private String[] violationLaws;
}
