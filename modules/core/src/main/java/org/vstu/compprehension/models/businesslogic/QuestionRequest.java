package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.entities.EnumData.SearchDirections;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {
    
    private List<Concept> deniedConcepts;

    private List<Concept> targetConcepts;

    /** Question Storage can treat this as "optional targets" or "preferred" */
    private List<Concept> allowedConcepts;

    private List<Law> targetLaws;
    
    private List<Law> deniedLaws;

    private List<Law> allowedLaws;

    private List<String> deniedQuestionNames;


    /**
     * Условная единица, показывающая долго или быстро решается вопрос
     * 1 - быстро, 10 - очень долго
     */
    private int solvingDuration;


    /**
     * Сложность задания [0..1]
     */
    private float complexity;

    /**
     * Направление поиска по сложности
     */
    private SearchDirections complexitySearchDirection;

    /**
     * Направление поиска по сложности
     */
    private SearchDirections lawsSearchDirection;

    /**
     * Probability of choosing an auto-generated question
     * Вероятность выбора авто-сгенерированного вопроса
     * range: [0..1]
     * 0: always pick manual question
     * 1: always pick auto-generated question
     */
    private double chanceToPickAutogeneratedQuestion = 1;

    ExerciseAttemptEntity exerciseAttempt;
}

