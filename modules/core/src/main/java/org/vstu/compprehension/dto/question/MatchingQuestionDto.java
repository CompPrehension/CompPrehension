package org.vstu.compprehension.dto.question;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.vstu.compprehension.dto.QuestionAnswerDto;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class MatchingQuestionDto extends QuestionDto {
    @Builder.Default
    private QuestionAnswerDto[] groups = new QuestionAnswerDto[0];
}
