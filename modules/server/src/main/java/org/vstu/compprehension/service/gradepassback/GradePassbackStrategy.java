package org.vstu.compprehension.service.gradepassback;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exerciseattempt.GradePassbackTargetData;

/**
 * Реализация отвечает за один механизм отправки оценки (LTI AGS, Moodle WS и т.п.).
 */
public interface GradePassbackStrategy {
    boolean supports(@NotNull GradePassbackTargetData target);

    /** @return true, если оценка успешно отправлена; false — при ошибке внутри стратегии. */
    boolean passGrade(@NotNull GradePassbackTargetData target, double grade);
}
