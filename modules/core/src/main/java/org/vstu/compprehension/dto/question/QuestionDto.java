package org.vstu.compprehension.dto.question;

import org.vstu.compprehension.models.data.questionoptions.QuestionOptionsData;
import lombok.Builder;
import org.vstu.compprehension.dto.AnswerDto;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.dto.QuestionAnswerDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class QuestionDto {
    private Long questionId;
    private Integer questionMetadataId;
    private String type;
    private String text;
    private QuestionOptionsData options;
    @Builder.Default
    private QuestionAnswerDto[] answers = new QuestionAnswerDto[0];
    private AnswerDto[] responses;
    private FeedbackDto feedback;
}
