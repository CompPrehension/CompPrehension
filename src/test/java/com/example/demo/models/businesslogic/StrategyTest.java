package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.EnumData.RoleInExercise;
import com.example.demo.models.entities.ExerciseAttempt;
import com.example.demo.models.entities.ExerciseConcept;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class StrategyTest {

    @Autowired
    private Strategy strategy;

    @Test
    @Disabled("Until ExerciseAttempt is filled")
    public void generateQuestionThreeTimes () throws Exception
    {
        assertNotNull(strategy);

        ExerciseAttempt testExerciseAttempt = new ExerciseAttempt();//Заполнить все значимые поля

        assertNotNull(testExerciseAttempt.getExercise());

        QuestionRequest qr = strategy.generateQuestionRequest(testExerciseAttempt);

        assertTrue(checkQuestionRequest(qr, testExerciseAttempt));

        QuestionRequest qr1 = strategy.generateQuestionRequest(testExerciseAttempt);

        assertTrue(checkQuestionRequest(qr1, testExerciseAttempt));

        QuestionRequest qr2 = strategy.generateQuestionRequest(testExerciseAttempt);

        assertTrue(checkQuestionRequest(qr2, testExerciseAttempt));

    }

    private boolean checkQuestionRequest(QuestionRequest qr, ExerciseAttempt testExerciseAttempt) {
        List<String> deniedConcepts = new ArrayList<>();
        List<String> targetConcepts = new ArrayList<>();

        //Выделить из упражнения целевые и запрещенные законы
        for (ExerciseConcept ec : testExerciseAttempt.getExercise().getExerciseConcepts()) {

            if (ec.getRoleInExercise() == RoleInExercise.TARGETED) {

                targetConcepts.add(ec.getConcept().getName());

            } else if (ec.getRoleInExercise() == RoleInExercise.FORBIDDEN) {

                deniedConcepts.add(ec.getConcept().getName());

            }
        }

        //Все целевые концепты должны быть внесены (а если лишние, но не противоречащие запрещённым?)
        if (!qr.getTargetConcepts().stream().map(i -> i.getName()).collect(Collectors.toList())
                .containsAll(targetConcepts)) {
            return false;
        }

        //все запрещённые концепты должны быть обозначены
        if (!deniedConcepts.containsAll(qr.getDeniedConcepts().stream().map(i -> i.getName()).collect(Collectors.toList()))) {
            return false;
        }

        for (String deniedConcept : deniedConcepts){
            //Среди целевых концептов запроса не должно быть запрещённых концептов задания
            if(qr.getTargetConcepts().stream().map(i -> i.getName()).collect(Collectors.toList()).contains(deniedConcept)){
                return false;
            }
        }

        return true;
    }
}