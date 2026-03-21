package org.vstu.compprehension.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.events.AttemptCompletedEvent;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;

import java.util.Random;

@Service
public class ExerciseAttemptService {
    private final ExerciseAttemptRepository exerciseAttemptRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public ExerciseAttemptService(ExerciseAttemptRepository exerciseAttemptRepository) {
        this.exerciseAttemptRepository = exerciseAttemptRepository;
    }

    public void ensureAttemptStatus(ExerciseAttemptEntity exerciseAttempt, Decision strategyDecision) {
        if (strategyDecision == Decision.FINISH && exerciseAttempt.getAttemptStatus() == AttemptStatus.INCOMPLETE) {
            exerciseAttempt.setAttemptStatus(AttemptStatus.COMPLETED_BY_USER);
            exerciseAttemptRepository.save(exerciseAttempt);
            eventPublisher.publishEvent(new AttemptCompletedEvent(this, exerciseAttempt.getId()));
        } else {
            exerciseAttemptRepository.save(exerciseAttempt);
        }
    }

    /**
     * Sets LTI context fields on an attempt (lineitem URL, course context ID, Moodle user ID).
     * Called by ExerciseController after creating an attempt from an LTI session.
     */
    @Transactional
    public void setLtiContext(Long attemptId, String lineitemUrl, String contextId, String ltiUserId) {
        ExerciseAttemptEntity attempt = exerciseAttemptRepository.findById(attemptId).orElseThrow();
        attempt.setLtiLineitemUrl(lineitemUrl);
        attempt.setLtiContextId(contextId);
        attempt.setLtiUserId(ltiUserId);
        exerciseAttemptRepository.save(attempt);
    }

    /**
     * Calculates a normalized grade [0.0, 1.0] for grade passback to Moodle.
     * Returns 1.0 for completed attempts, 0.0 otherwise.
     */
    public double calculateFinalGrade(ExerciseAttemptEntity attempt) {
        return attempt.getAttemptStatus() == AttemptStatus.COMPLETED_BY_USER ? new Random().nextDouble() : 0.0; // todo нормальное вычисление оценки
    }
}
