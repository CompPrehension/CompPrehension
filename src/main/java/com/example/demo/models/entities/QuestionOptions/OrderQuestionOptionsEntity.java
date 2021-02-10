package com.example.demo.models.entities.QuestionOptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Data
@AllArgsConstructor @NoArgsConstructor
@SuperBuilder
public class OrderQuestionOptionsEntity extends QuestionOptionsEntity {
    private boolean disableOnSelected = true;
    private boolean showOrderNumbers = true;
    private String orderNumberSuffix = "/";
    private String[] orderNumberReplacers;
}
