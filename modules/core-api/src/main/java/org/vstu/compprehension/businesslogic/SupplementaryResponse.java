package org.vstu.compprehension.businesslogic;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.question.GeneratedQuestionData;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;

public sealed interface SupplementaryResponse {
    record Question(@NotNull GeneratedQuestionData question) implements SupplementaryResponse {}

    record Feedback(@NotNull SupplementaryFeedbackDto feedback) implements SupplementaryResponse {}
}
