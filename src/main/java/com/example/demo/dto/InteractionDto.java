package com.example.demo.dto;

import lombok.Data;

@Data
public class InteractionDto {
    private Long attemptId;
    private Long questionId;
    private String answers;
}
