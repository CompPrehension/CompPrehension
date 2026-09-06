package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;

import java.util.List;

/**
 * Попытка в объёме, который уезжает на фронт.
 * <p>
 * Раньше на её месте по коду ходила {@code ExerciseAttemptEntity}, а нужные шесть полей
 * снимались с неё в {@code Mapper.toDto}. Ради одного списка идентификаторов запрос тянул
 * {@code left join fetch a.questions} — то есть все вопросы попытки целиком.
 *
 * @param courseId    null, если попытка запущена вне курса
 * @param questionIds основные вопросы попытки в порядке выдачи; вспомогательные исключены
 */
public record AttemptSummaryData(
        long attemptId,
        long userId,
        long exerciseId,
        @Nullable Long courseId,
        @NotNull AttemptStatus status,
        @NotNull List<Long> questionIds) {
}
