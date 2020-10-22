package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.Exercise;
import com.example.demo.models.entities.ExerciseAttempt;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class StrategyTest {

    @Test
    void generateQuestionRequest() {
        ExerciseAttempt attempt = new ExerciseAttempt();
        Exercise exercise = new Exercise();
        exercise.setDomain(new TestDomain(this.domainRepository.findAll().next()));

        Strategy strategy = new Strategy();

        QuestionRequest qr = strategy.generateQuestionRequest(attempt);
        Assert.assertTrue(qr.getTargetLaws().isEmpty());
    }
}