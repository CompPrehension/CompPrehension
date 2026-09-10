package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.QuestionMetadataBitsData;

import java.util.List;

public record AttemptQuestionData(
        long questionId,
        @Nullable String name,
        @Nullable String domainType,
        @Nullable QuestionMetadataBitsData metadata,
        @NotNull List<AttemptQuestionInteractionData> interactions) {
}
