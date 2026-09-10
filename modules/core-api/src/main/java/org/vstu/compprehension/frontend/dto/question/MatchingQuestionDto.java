package org.vstu.compprehension.frontend.dto.question;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.vstu.compprehension.frontend.dto.QuestionAnswerDto;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class MatchingQuestionDto extends QuestionDto {
    @Builder.Default
    private QuestionAnswerDto[] groups = new QuestionAnswerDto[0];
}
