package org.vstu.compprehension.businesslogic;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.GeneratedQuestionData;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;

public class SupplementaryResponse {
    @Getter
    @Nullable private GeneratedQuestionData question;
    @Getter
    @Nullable private SupplementaryFeedbackDto feedback;

    public SupplementaryResponse(@Nullable GeneratedQuestionData supplementaryQuestion) {
        this.question = supplementaryQuestion;
        this.feedback = null;
    }

    public SupplementaryResponse(@Nullable SupplementaryFeedbackDto supplementaryFeedback) {
        this.question = null;
        this.feedback = supplementaryFeedback;
    }
}
