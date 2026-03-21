package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;

/**
 * Out-port: отправка итоговой оценки в gradebook.
 * Реализация выбирает конкретный механизм (LTI AGS, Moodle WS и т.д.) на основе данных attempt'а.
 */
public interface GradePassbackService {
    void passGrade(ExerciseAttemptEntity attempt);
}
