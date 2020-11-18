package com.example.demo.models.businesslogic;

import com.example.demo.DemoApplication;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.RoleInExercise;
import com.example.demo.models.entities.ExerciseAttempt;
import com.example.demo.models.entities.ExerciseConcept;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.demo.models.repository.ExerciseAttemptRepository;
import org.apache.commons.collections4.IterableUtils;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes= DemoApplication.class)
@Transactional
public class StrategyTest {
    @Autowired
    private AbstractStrategy strategy;
    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;
    @Autowired
    ProgrammingLanguageExpressionDomain domain;
    @Test
    public void generateQuestionThreeTimes () throws Exception
    {
        assertNotNull(strategy);

        List<ExerciseAttempt> testExerciseAttemptList = IterableUtils.toList( exerciseAttemptRepository.findAll());//Заполнить все значимые поля

        ExerciseAttempt testExerciseAttempt = testExerciseAttemptList.get(0);

        assertNotNull(testExerciseAttempt.getExercise());

        QuestionRequest qr = strategy.generateQuestionRequest(testExerciseAttempt);

        assertTrue(checkQuestionRequest(qr, testExerciseAttempt));

        Question q = domain.makeQuestion(qr, Language.ENGLISH);
        assertFalse(q.getQuestionText().getText().isEmpty());
        //Вызов домена с проверкой адекватности вопросов и тд

        QuestionRequest qr1 = strategy.generateQuestionRequest(testExerciseAttempt);

        assertTrue(checkQuestionRequest(qr1, testExerciseAttempt));

        Question q1 = domain.makeQuestion(qr1, Language.ENGLISH);
        assertFalse(q1.getQuestionText().getText().isEmpty());
        //Вызов домена с проверкой адекватности вопросов и тд

        QuestionRequest qr2 = strategy.generateQuestionRequest(testExerciseAttempt);

        assertTrue(checkQuestionRequest(qr2, testExerciseAttempt));

        Question q2 = domain.makeQuestion(qr2, Language.ENGLISH);
        assertFalse(q2.getQuestionText().getText().isEmpty());
        //Вызов домена с проверкой адекватности вопросов и тд

    }

    private boolean checkQuestionRequest(QuestionRequest qr, ExerciseAttempt testExerciseAttempt) {
        List<String> deniedConcepts = new ArrayList<>();
        List<String> targetConcepts = new ArrayList<>();

        //Выделить из упражнения целевые и запрещенные законы
        for (ExerciseConcept ec : testExerciseAttempt.getExercise().getExerciseConcepts()) {

            if (ec.getRoleInExercise() == RoleInExercise.TARGETED) {

                targetConcepts.add(ec.getConceptName());

            } else if (ec.getRoleInExercise() == RoleInExercise.FORBIDDEN) {

                deniedConcepts.add(ec.getConceptName());

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