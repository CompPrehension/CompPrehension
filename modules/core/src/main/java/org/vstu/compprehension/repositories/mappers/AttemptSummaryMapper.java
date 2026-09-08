package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exerciseattempt.AttemptSummaryData;
import org.vstu.compprehension.mappers.Mapping;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository.AttemptSummaryRow;

import java.util.List;

/**
 * Попытка в объёме, который уезжает на фронт.
 * <p>
 * Идентификаторы вопросов приходят отдельным запросом: маппер сам в базу не ходит.
 */
public interface AttemptSummaryMapper extends Mapping {

    @NotNull AttemptSummaryData map(@NotNull AttemptSummaryRow row, @NotNull List<Long> questionIds);
}
