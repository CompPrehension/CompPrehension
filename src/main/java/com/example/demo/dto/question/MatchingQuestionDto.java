package com.example.demo.dto.question;

import com.example.demo.dto.QuestionAnswerDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class MatchingQuestionDto extends QuestionDto {
    private QuestionAnswerDto[] groups = new QuestionAnswerDto[0];
}
