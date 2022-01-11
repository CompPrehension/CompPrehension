package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.entities.EnumData.SearchDirections;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
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

    /**
     * Направление поиска по сложности
     */
    private SearchDirections complexitySearchDirection;

    /**
     * Направление поиска по сложности
     */
    private SearchDirections lawsSearchDirection;

    ExerciseAttemptEntity exerciseAttempt;
}

