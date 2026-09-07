package org.vstu.compprehension.adapters;

import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.services.AuthService;
import org.vstu.compprehension.services.GradePassbackService;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemRole;
import org.vstu.compprehension.data.exerciseattempt.GradePassbackTargetData;
import org.vstu.compprehension.businesslogic.auth.PermissionScope;
import org.vstu.compprehension.repositories.data.ExerciseAttemptDataRepository;
import org.vstu.compprehension.service.gradepassback.GradePassbackStrategy;

import java.util.List;

/**
 * Реализует out-port {@link GradePassbackService}, делегируя первой подходящей стратегии.
 */
@Service
@Log4j2
public class GradePassbackServiceImpl implements GradePassbackService {

    private final List<GradePassbackStrategy> strategies;
    /**
     * Адресат оценки перечитывается по идентификатору: метод асинхронный и работает
     * в своей транзакции, так что объект, собранный вызывающим, здесь не годится.
     * <p>
     * Через {@code ExerciseAttemptService} получался цикл бинов, который приходилось
     * разрывать {@code @Lazy}: ExerciseAttemptService -> GradePassbackService ->
     * ExerciseAttemptService.
     */
    private final ExerciseAttemptDataRepository exerciseAttemptDataRepository;
    private final AuthService authService;

    public GradePassbackServiceImpl(
            List<GradePassbackStrategy> strategies,
            ExerciseAttemptDataRepository exerciseAttemptDataRepository,
            AuthService authService
    ) {
        this.strategies = strategies;
        this.exerciseAttemptDataRepository = exerciseAttemptDataRepository;
        this.authService = authService;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void passGrade(long attemptId, double grade) {
        GradePassbackTargetData target = exerciseAttemptDataRepository
                .findGradePassbackTarget(attemptId).orElse(null);
        if (target == null) {
            log.warn("Attempt {} not found for grade passback", attemptId);
            return;
        }

        long userId = target.userId();
        Long courseId = target.course() == null ? null : target.course().courseId();
        boolean isStudent = courseId != null
                && authService.hasRole(userId, SystemRole.STUDENT, PermissionScope.course(courseId));
        if (!isStudent) {
            log.info("Skipping grade passback for attempt {}: user {} is not a STUDENT in course {}",
                    attemptId, userId, courseId);
            return;
        }

        boolean anyStrategySupported = false;
        for (GradePassbackStrategy s : strategies) {
            if (s.supports(target)) {
                anyStrategySupported = true;
                String strategyName = s.getClass().getSimpleName();
                log.info("Grade passback for attempt {} via {}", attemptId, strategyName);
                boolean sent = s.passGrade(target, grade);
                if (sent) {
                    log.info("Grade passback success for attempt {} via {}", attemptId, strategyName);
                } else {
                    log.warn("Grade passback via {} failed for attempt {}", strategyName, attemptId);
                }
            }
        }
        if (!anyStrategySupported) {
            log.warn("No suitable grade passback strategy found for attempt {}", attemptId);
        }
    }
}
