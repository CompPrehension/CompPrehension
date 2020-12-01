package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.BackendFact;
import com.example.demo.utils.DomainAdapter;

import java.util.List;

public class Ordering extends Question {

    public Ordering(com.example.demo.models.entities.Question questionData) {
        super(questionData);
    }

    @Override
    public List<BackendFact> responseToFacts() {
        return DomainAdapter.getDomain(questionData.getDomainEntity().getName()).responseToFacts(
                getQuestionDomainType(),
                super.studentResponses,
                getAnswerObjects()
        );
    }

    @Override
    public List<BackendFact> responseToFacts(long backendId) {
        return null;
    }

    @Override
    public Long getExerciseAttemptId() {
        return null;
    }
}
