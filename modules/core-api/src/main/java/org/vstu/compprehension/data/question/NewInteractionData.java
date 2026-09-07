package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.InteractionType;

import java.util.List;

public record NewInteractionData(
        long questionId,
        @NotNull InteractionType interactionType,
        @NotNull List<Long> carriedResponseIds,
        @NotNull List<SubmittedAnswerData> answers,
        @NotNull List<ViolationData> violations,
        @NotNull List<String> correctLaws,
        int interactionsLeft) {
}
