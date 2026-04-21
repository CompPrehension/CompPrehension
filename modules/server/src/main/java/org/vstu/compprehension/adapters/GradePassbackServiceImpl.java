package org.vstu.compprehension.adapters;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.Service.GradePassbackService;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.service.gradepassback.GradePassbackStrategy;

import java.util.List;

/**
 * Реализует out-port {@link GradePassbackService}, делегируя первой подходящей стратегии.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class GradePassbackServiceImpl implements GradePassbackService {

    private final List<GradePassbackStrategy> strategies;
    private final ExerciseAttemptRepository exerciseAttemptRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void passGrade(ExerciseAttemptEntity attempt, double grade) {
        ExerciseAttemptEntity fresh = exerciseAttemptRepository
                .findById(attempt.getId()).orElse(null);
        if (fresh == null) {
            log.warn("Attempt {} not found for grade passback", attempt.getId());
            return;
        }

        boolean anyStrategySupported = false;
        for (GradePassbackStrategy s : strategies) {
            if (s.supports(fresh)) {
                anyStrategySupported = true;
                String strategyName = s.getClass().getSimpleName();
                log.info("Grade passback for attempt {} via {}", fresh.getId(), strategyName);
                boolean sent = s.passGrade(fresh, grade);
                if (sent) {
                    log.info("Grade passback success for attempt {} via {}", fresh.getId(), strategyName);
                } else {
                    log.warn("Grade passback via {} failed for attempt {}", strategyName, fresh.getId());
                }
            }
        }
        if (!anyStrategySupported) {
            log.warn("No suitable grade passback strategy found for attempt {}", fresh.getId());
        }
    }
}
