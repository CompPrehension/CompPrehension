package org.vstu.compprehension.dto.question;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class OrderQuestionDto extends QuestionDto {
    private String[] initialTrace;
}
