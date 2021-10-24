package org.vstu.compprehension.dto.question;

import org.vstu.compprehension.dto.AnswerDto;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.dto.QuestionAnswerDto;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class QuestionDto {
    private Long attemptId;
    private Long questionId;
    private String type;
    private String text;
    private QuestionOptionsEntity options;
    private QuestionAnswerDto[] answers = new QuestionAnswerDto[0];
    private AnswerDto[] responses;
    private FeedbackDto feedback;
}
