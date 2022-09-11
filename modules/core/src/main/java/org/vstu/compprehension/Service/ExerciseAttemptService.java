package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseAttemptService {
    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;

    public void ensureAttemptStatus(ExerciseAttemptEntity exerciseAttempt, Decision strategyDecision) {
        if (strategyDecision == Decision.FINISH && exerciseAttempt.getAttemptStatus() == AttemptStatus.INCOMPLETE) {
            exerciseAttempt.setAttemptStatus(AttemptStatus.COMPLETED_BY_USER);
        }
        exerciseAttemptRepository.save(exerciseAttempt);
    }
}
