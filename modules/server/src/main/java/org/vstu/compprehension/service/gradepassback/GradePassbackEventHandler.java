package org.vstu.compprehension.service.gradepassback;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemRole;
import org.vstu.compprehension.businesslogic.auth.PermissionScope;
import org.vstu.compprehension.data.outbox.AttemptFinishedEvent;
import org.vstu.compprehension.repositories.data.ExerciseAttemptDataRepository;
import org.vstu.compprehension.service.outbox.OutboxEventHandler;
import org.vstu.compprehension.services.AuthService;

import java.util.List;

/**
 * Отправляет оценку завершённой попытки во внешнюю систему подходящими стратегиями.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class GradePassbackEventHandler implements OutboxEventHandler<AttemptFinishedEvent> {

    private final List<GradePassbackStrategy> strategies;
    private final ExerciseAttemptDataRepository exerciseAttemptDataRepository;
    private final AuthService authService;

    @Override
    public void handle(@NotNull AttemptFinishedEvent event) {
        long attemptId = event.attemptId();
        var target = exerciseAttemptDataRepository.findGradePassbackTarget(attemptId).orElse(null);
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
        for (GradePassbackStrategy strategy : strategies) {
            if (strategy.supports(target)) {
                anyStrategySupported = true;
                String strategyName = strategy.getClass().getSimpleName();
                log.info("Grade passback for attempt {} via {}", attemptId, strategyName);
                try {
                    strategy.passGrade(target, event.grade(), event.finishedAt());
                } catch (RuntimeException ex) {
                    throw new IllegalStateException(
                            "Grade passback via " + strategyName + " failed for attempt " + attemptId + ": " + ex.getMessage(),
                            ex);
                }
                log.info("Grade passback success for attempt {} via {}", attemptId, strategyName);
            }
        }
        if (!anyStrategySupported) {
            log.warn("No suitable grade passback strategy found for attempt {}", attemptId);
        }
    }
}
