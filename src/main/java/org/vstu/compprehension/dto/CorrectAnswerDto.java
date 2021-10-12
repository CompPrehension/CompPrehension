package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CorrectAnswerDto {
    private AnswerDto[] answers;
    private String explanation;
}
