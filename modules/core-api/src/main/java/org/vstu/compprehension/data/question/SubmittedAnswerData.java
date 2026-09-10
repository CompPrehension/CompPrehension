package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.Nullable;

public record SubmittedAnswerData(
        int leftAnswerId,
        int rightAnswerId,
        @Nullable Long createdByInteractionId) {
}
