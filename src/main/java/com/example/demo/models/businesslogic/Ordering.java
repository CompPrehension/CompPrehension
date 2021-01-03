package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.BackendFactEntity;
import com.example.demo.models.entities.QuestionEntity;
import com.example.demo.utils.DomainAdapter;

import java.util.List;

public class Ordering extends Question {

    public Ordering(QuestionEntity questionData) {
        super(questionData);
    }

    @Override
    public List<BackendFactEntity> responseToFacts() {
        return DomainAdapter.getDomain(questionData.getDomainEntity().getName()).responseToFacts(
                getQuestionDomainType(),
                super.studentResponses,
                getAnswerObjects()
        );
    }

    @Override
    public List<BackendFactEntity> responseToFacts(long backendId) {
        return null;
    }

    @Override
    public Long getExerciseAttemptId() {
        return null;
    }
}
