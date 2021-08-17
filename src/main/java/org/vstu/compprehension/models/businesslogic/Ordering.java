package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;
import org.vstu.compprehension.utils.DomainAdapter;

import java.util.List;

public class Ordering extends Question {

    public Ordering(QuestionEntity questionData) {
        super(questionData);
    }

    @Override
    public List<BackendFactEntity> responseToFacts(List<ResponseEntity> responses) {
        return DomainAdapter.getDomain(questionData.getDomainEntity().getClassPath()).responseToFacts(
                getQuestionDomainType(),
                responses,
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
