package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.QuestionMetadataBitsData;

import java.util.List;

/**
 * Вопрос, заданный в рамках попытки.
 *
 * @param metadata     null для вопросов, сгенерированных без банка заданий
 * @param interactions в порядке возрастания id, то есть в хронологическом порядке
 */
public record AttemptQuestionData(
        long id,
        @Nullable String name,
        @Nullable String domainType,
        @Nullable QuestionMetadataBitsData metadata,
        @NotNull List<AttemptInteractionData> interactions) {
}
