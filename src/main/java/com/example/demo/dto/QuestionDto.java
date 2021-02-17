package com.example.demo.dto;

import com.example.demo.models.entities.QuestionOptions.QuestionOptionsEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class QuestionDto {
    private String id;
    private String type;
    private String text;
    private QuestionOptionsEntity options;
    private QuestionAnswerDto[] answers = new QuestionAnswerDto[0];
}
