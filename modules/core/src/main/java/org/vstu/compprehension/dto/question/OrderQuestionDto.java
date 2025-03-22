package org.vstu.compprehension.dto.question;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter @Setter
@SuperBuilder
public class OrderQuestionDto extends QuestionDto {
    private String[] initialTrace;
}
