package org.vstu.compprehension.dto.question;

import lombok.Builder;
import org.vstu.compprehension.dto.QuestionAnswerDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class MatchingQuestionDto extends QuestionDto {
    @Builder.Default
    private QuestionAnswerDto[] groups = new QuestionAnswerDto[0];
}
