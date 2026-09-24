package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.Nullable;

public sealed interface SubmittedAnswerData permits SubmittedAnswerData.Pair, SubmittedAnswerData.Choice {

    int leftAnswerId();

    @Nullable Long createdByInteractionId();

    record Pair(int leftAnswerId, int rightAnswerId, @Nullable Long createdByInteractionId)
            implements SubmittedAnswerData {
    }

    record Choice(int leftAnswerId, int value, @Nullable Long createdByInteractionId)
            implements SubmittedAnswerData {
    }
}
