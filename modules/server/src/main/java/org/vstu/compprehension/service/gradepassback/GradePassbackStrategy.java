package org.vstu.compprehension.service.gradepassback;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.data.GradePassbackTargetData;

/**
 * Реализация отвечает за один механизм отправки оценки (LTI AGS, Moodle WS и т.п.).
 * <p>
 * Принимает данные, а не {@code ExerciseAttemptEntity}: отправка асинхронная и идёт
 * в своей транзакции, поэтому обходить ленивые связи попытки здесь нельзя.
 */
public interface GradePassbackStrategy {
    boolean supports(@NotNull GradePassbackTargetData target);

    /** @return true, если оценка успешно отправлена; false — при ошибке внутри стратегии. */
    boolean passGrade(@NotNull GradePassbackTargetData target, double grade);
}
