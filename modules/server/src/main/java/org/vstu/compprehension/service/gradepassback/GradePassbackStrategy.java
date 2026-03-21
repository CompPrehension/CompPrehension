package org.vstu.compprehension.service.gradepassback;

import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;

/**
 * Внутренняя стратегия отправки оценки.
 * Каждая реализация отвечает за один механизм (LTI AGS, Moodle WS и т.д.).
 */
public interface GradePassbackStrategy {
    /** Возвращает true если данная стратегия применима для указанного attempt. */
    // todo определять тип попытки и проставлять enum, затем брать по объекту enum'а
    boolean supports(ExerciseAttemptEntity attempt);

    /** Отправляет оценку. Вызывается только если {@link #supports} вернул true. */
    void passGrade(ExerciseAttemptEntity attempt);
}
