package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.strategies.StrategyDecision;
import org.vstu.compprehension.data.exerciseattempt.AttemptStageData;

/** Прохождение этапов упражнения в попытке. */
public interface AttemptStageService {

    /** Этап, для которого задаётся следующий вопрос попытки. */
    @NotNull AttemptStageData findCurrentStage(long attemptId);

    /** Оценка попытки после последнего записанного ответа и решение, закончена ли она. */
    @NotNull StrategyDecision gradeAndDecide(long attemptId);
}
