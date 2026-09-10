package org.vstu.compprehension.frontend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CorrectAnswerDto {
    private AnswerDto[] answers;
    private String explanation;
}
