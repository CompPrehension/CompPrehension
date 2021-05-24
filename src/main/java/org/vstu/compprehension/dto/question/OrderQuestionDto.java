package org.vstu.compprehension.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class OrderQuestionDto extends QuestionDto {
    private String[] initialTrace;
}
