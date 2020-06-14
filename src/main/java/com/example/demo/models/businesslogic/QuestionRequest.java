package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.Law;

import java.util.ArrayList;
import java.util.List;

public class QuestionRequest {
    
    private List<Concept> deniedConcepts;
    
    private List<Concept> targetConcepts;
    
    private List<Law> targetLaws;
    
    private List<Law> deniedLaws;


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

