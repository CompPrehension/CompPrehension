package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;

import java.util.List;

public class Ordering extends Question {

    public Ordering(QuestionEntity questionData, Domain domain) {
        super(questionData, domain);
    }

    public List<BackendFactEntity> responseToFacts(List<ResponseEntity> responses) {
        return domain.responseToFacts(
                getQuestionDomainType(),
                responses,
                getAnswerObjects()
        );
    }

    public List<BackendFactEntity> responseToFacts(long backendId) {
        return null;
    }

    public Long getExerciseAttemptId() {
        return null;
    }
}
