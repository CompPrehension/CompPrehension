package org.vstu.compprehension.adapters;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.Service.AuthService;
import org.vstu.compprehension.Service.ExerciseAttemptService;
import org.vstu.compprehension.Service.GradePassbackService;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.SystemRole;
import org.vstu.compprehension.models.entities.EnumData.PermissionScope;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.service.gradepassback.GradePassbackStrategy;

import java.util.List;

/**
 * Реализует out-port {@link GradePassbackService}, делегируя первой подходящей стратегии.
 */
@Service
@Log4j2
public class GradePassbackServiceImpl implements GradePassbackService {

    private final List<GradePassbackStrategy> strategies;
    private final ExerciseAttemptService exerciseAttemptService;
    private final AuthService authService;

    public GradePassbackServiceImpl(
            List<GradePassbackStrategy> strategies,
            @Lazy ExerciseAttemptService exerciseAttemptService,
            AuthService authService
    ) {
        this.strategies = strategies;
        this.exerciseAttemptService = exerciseAttemptService;
        this.authService = authService;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void passGrade(ExerciseAttemptEntity attempt, double grade) {
        ExerciseAttemptEntity fresh = exerciseAttemptService
                .findById(attempt.getId()).orElse(null);
        if (fresh == null) {
            log.warn("Attempt {} not found for grade passback", attempt.getId());
            return;
        }

        Long userId = fresh.getUser().getId();
        Long courseId = fresh.getCourse() != null ? fresh.getCourse().getId() : null;
        boolean isStudent = courseId != null
                && authService.hasRole(userId, SystemRole.STUDENT, PermissionScope.course(courseId));
        if (!isStudent) {
            log.info("Skipping grade passback for attempt {}: user {} is not a STUDENT in course {}",
                    fresh.getId(), userId, courseId);
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
