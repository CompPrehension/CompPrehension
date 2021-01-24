package com.example.demo.dto;

import lombok.Data;

@Data
public class QuestionDto {
    private String id;
    private Integer type;
    private String text;
    private QuestionOptionsDto options;
    private QuestionAnswerDto[] answers = new QuestionAnswerDto[0];
}
