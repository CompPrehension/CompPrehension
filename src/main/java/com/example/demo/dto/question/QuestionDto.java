package com.example.demo.dto.question;

import com.example.demo.dto.QuestionAnswerDto;
import com.example.demo.models.entities.QuestionOptions.QuestionOptionsEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class QuestionDto {
    private String attemptId;
    private String questionId;
    private String type;
    private String text;
    private QuestionOptionsEntity options;
    private QuestionAnswerDto[] answers = new QuestionAnswerDto[0];
}
