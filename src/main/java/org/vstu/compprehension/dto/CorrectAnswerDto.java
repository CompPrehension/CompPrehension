package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CorrectAnswerDto {
    private Long[][] answers;
    private String explanation;
}
