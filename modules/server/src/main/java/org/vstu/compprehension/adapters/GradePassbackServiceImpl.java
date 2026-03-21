package org.vstu.compprehension.adapters;

import org.springframework.stereotype.Service;
import org.vstu.compprehension.Service.GradePassbackService;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.service.gradepassback.GradePassbackStrategy;

import java.util.List;

/**
 * Реализует out-port {@link GradePassbackService}, делегируя первой подходящей стратегии.
 * Стратегии упорядочены через {@code @Order} — LTI AGS проверяется первой.
 */
@Service
public class GradePassbackServiceImpl implements GradePassbackService {

    private final List<GradePassbackStrategy> strategies;

    public GradePassbackServiceImpl(List<GradePassbackStrategy> strategies) {
        this.strategies = strategies;
    }

    @Override
    public void passGrade(ExerciseAttemptEntity attempt) {
        strategies.stream()
                .filter(s -> s.supports(attempt))
                .findFirst()
                .ifPresent(s -> s.passGrade(attempt));
    }
}
