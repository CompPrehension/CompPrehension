package org.vstu.compprehension.service.gradepassback;

import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;

/** Реализация отвечает за один механизм отправки оценки (LTI AGS, Moodle WS и т.п.). */
public interface GradePassbackStrategy {
    boolean supports(ExerciseAttemptEntity attempt);

    /** @return true, если оценка успешно отправлена; false — при ошибке внутри стратегии. */
    boolean passGrade(ExerciseAttemptEntity attempt, double grade);
}
