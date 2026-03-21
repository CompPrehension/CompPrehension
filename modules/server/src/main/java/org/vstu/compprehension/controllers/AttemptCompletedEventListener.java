package org.vstu.compprehension.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.vstu.compprehension.Service.GradePassbackService;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.events.AttemptCompletedEvent;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;

/**
 * Слушает {@link AttemptCompletedEvent} и инициирует отправку оценки через {@link GradePassbackService}.
 *
 * <p>Тройная аннотация необходима:
 * <ul>
 *   <li>{@code @TransactionalEventListener(AFTER_COMMIT)} — срабатывает только после коммита,
 *       гарантируя что ltiLineitemUrl и остальные поля attempt видны в БД.</li>
 *   <li>{@code @Async} — не блокирует HTTP-ответ пользователю.</li>
 *   <li>{@code @Transactional(REQUIRES_NEW)} — открывает свою транзакцию в async-потоке,
 *       необходимую для lazy-loading связанных сущностей (user, exercise).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class AttemptCompletedEventListener {

    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final GradePassbackService gradePassbackService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAttemptCompleted(AttemptCompletedEvent event) {
        ExerciseAttemptEntity attempt = exerciseAttemptRepository
                .findById(event.getAttemptId()).orElse(null);
        if (attempt == null) {
            log.warn("Attempt {} not found on completion event", event.getAttemptId());
            return;
        }
        gradePassbackService.passGrade(attempt);
    }
}
