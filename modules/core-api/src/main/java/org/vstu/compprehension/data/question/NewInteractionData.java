package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.InteractionType;

import java.util.List;

public record NewInteractionData(
        long questionId,
        @NotNull InteractionType interactionType,
        @NotNull List<NewInteractionAnswerData> answers,
        @NotNull List<ViolationData> violations,
        @NotNull List<String> correctLaws,
        @NotNull List<AnswerHypothesisData> hypotheses,
        @Nullable HypothesisClarificationData clarification,
        int interactionsLeft) {
}
