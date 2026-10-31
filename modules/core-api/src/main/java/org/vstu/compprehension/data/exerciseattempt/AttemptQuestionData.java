package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.QuestionMetadataBitsData;

import java.util.List;

/**
 * @param stageIndex этап упражнения, для которого вопрос задан; у наводящего вопроса этапа нет
 */
public record AttemptQuestionData(
        long questionId,
        @Nullable String name,
        @Nullable String domainType,
        @Nullable QuestionMetadataBitsData metadata,
        @Nullable Integer stageIndex,
        @NotNull List<AttemptQuestionInteractionData> interactions) {

    /** Студент дошёл до конца вопроса: шагов в нём не осталось. */
    public boolean isCompleted() {
        if (interactions.isEmpty()) {
            return false;
        }
        var stepsLeft = interactions.getLast().interactionsLeft();
        return stepsLeft != null && stepsLeft == 0;
    }
}
