package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
