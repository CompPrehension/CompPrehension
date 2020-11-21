package com.example.demo.models.businesslogic;

import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {
    
    private List<Concept> deniedConcepts;

    private List<Concept> targetConcepts;

    private List<Concept> allowedConcepts;

    private List<Law> targetLaws;
    
    private List<Law> deniedLaws;

    private List<Law> allowedLaws;


    /**
     * Условная единица, показывающая долго или быстро решается вопрос
     * 1 - быстро, 10 - очень долго
     */
    private int solvingDuration;


    /**
     * Сложность задания
     */
    private int complexity;
    
    
}

