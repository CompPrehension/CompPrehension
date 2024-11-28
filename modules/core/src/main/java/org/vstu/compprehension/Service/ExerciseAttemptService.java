package org.vstu.compprehension.Service;

import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;

@Service
public class ExerciseAttemptService {
    private final ExerciseAttemptRepository exerciseAttemptRepository;

    public ExerciseAttemptService(ExerciseAttemptRepository exerciseAttemptRepository) {
        this.exerciseAttemptRepository = exerciseAttemptRepository;
    }

    public void ensureAttemptStatus(ExerciseAttemptEntity exerciseAttempt, Decision strategyDecision) {
        if (strategyDecision == Decision.FINISH && exerciseAttempt.getAttemptStatus() == AttemptStatus.INCOMPLETE) {
            exerciseAttempt.setAttemptStatus(AttemptStatus.COMPLETED_BY_USER);
        }
        exerciseAttemptRepository.save(exerciseAttempt);
    }
}
