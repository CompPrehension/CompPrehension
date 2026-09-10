package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.InteractionType;

import java.util.List;

public record AttemptQuestionInteractionData(
        long interactionId,
        int orderNumber,
        @Nullable InteractionType type,
        @Nullable Integer interactionsLeft,
        @NotNull List<String> violationLawNames,
        @NotNull List<String> correctLawNames) {
}
