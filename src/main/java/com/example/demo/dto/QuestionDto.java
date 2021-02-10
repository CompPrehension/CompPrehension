package com.example.demo.dto;

import com.example.demo.models.entities.QuestionOptions.QuestionOptionsEntity;
import lombok.Data;

@Data
public class QuestionDto {
    private String id;
    private String type;
    private String text;
    private QuestionOptionsEntity options;
    private QuestionAnswerDto[] answers = new QuestionAnswerDto[0];
}
