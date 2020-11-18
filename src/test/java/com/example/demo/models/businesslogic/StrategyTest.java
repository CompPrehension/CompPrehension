package com.example.demo.models.businesslogic;

import com.example.demo.DemoApplication;
import com.example.demo.models.businesslogic.AbstractStrategy;
import com.example.demo.models.businesslogic.QuestionRequest;
import com.example.demo.models.entities.EnumData.RoleInExercise;
import com.example.demo.models.entities.ExerciseAttempt;
import com.example.demo.models.entities.ExerciseConcept;
import com.example.demo.models.repository.ExerciseAttemptRepository;
import org.apache.commons.collections4.IterableUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes= DemoApplication.class)
@Transactional
public class StrategyTest {
    @Autowired
    private AbstractStrategy strategy;
    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;
    @Test
    public void generateQuestionThreeTimes ()throws Exception
    {

        List<ExerciseAttempt> testExerciseAttemptList = IterableUtils.toList( exerciseAttemptRepository.findAll());//Заполнить все значимые поля

        ExerciseAttempt testExerciseAttempt = testExerciseAttemptList.get(0);

        QuestionRequest qr = strategy.generateQuestionRequest(testExerciseAttempt);

        Assert.assertTrue(checkQuestionRequest(qr, testExerciseAttempt));

        //Вызов домена с проверкой адекватности вопросов и тд

        QuestionRequest qr1 = strategy.generateQuestionRequest(testExerciseAttempt);

        Assert.assertTrue(checkQuestionRequest(qr1, testExerciseAttempt));

        //Вызов домена с проверкой адекватности вопросов и тд

        QuestionRequest qr2 = strategy.generateQuestionRequest(testExerciseAttempt);

        Assert.assertTrue(checkQuestionRequest(qr2, testExerciseAttempt));

        //Вызов домена с проверкой адекватности вопросов и тд

    }

    private boolean checkQuestionRequest(QuestionRequest qr, ExerciseAttempt testExerciseAttempt) {
        List<Long> deniedConcepts = new ArrayList<>();
        List<Long> targetConcepts = new ArrayList<>();

        //Выделить из упражнения целевые и запрещенные законы
        for (ExerciseConcept ec : testExerciseAttempt.getExercise().getExerciseConcepts()) {

            if (ec.getRoleInExercise() == RoleInExercise.TARGETED) {

                targetConcepts.add(ec.getConcept().getId());

            } else if (ec.getRoleInExercise() == RoleInExercise.FORBIDDEN) {

                deniedConcepts.add(ec.getConcept().getId());

            }
        }

        //Все целевые концепты должны быть внесены (а если лишние, но не противоречащие запрещённым?)!!!МИНИМУМ 1!!!
        if (!qr.getTargetConcepts().stream().map(i -> i.getId()).collect(Collectors.toList())
                .containsAll(targetConcepts)) {
            return false;
        }

        //все запрещённые концепты должны быть обозначены
        if (!deniedConcepts.containsAll(qr.getDeniedConcepts().stream().map(i -> i.getId()).collect(Collectors.toList()))) {
            return false;
        }

        for (Long deniedConcept:deniedConcepts){
            //Среди целевых концептов запроса не должно быть запрещённых концептов задания
            if(qr.getTargetConcepts().stream().map(i -> i.getId()).collect(Collectors.toList()).contains(deniedConcept)){
                return false;
            }
        }

        return true;
    }
}