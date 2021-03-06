package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackDto {
    private float grade;
    private String[] errors;
}

