package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record RecordedInteractionData(
        long interactionId,
        int correctInteractionsCount,
        int erroneousInteractionsCount,
        @NotNull List<ResponseData> responses,
        @Nullable InteractionResponsesData latestCorrectInteraction) {
}
