package org.vstu.compprehension.models.businesslogic;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.dto.SupplementaryFeedbackDto;

public class SupplementaryResponse {
    @Getter
    @Nullable private Question question;
    @Getter
    @Nullable private SupplementaryFeedbackDto feedback;

    public SupplementaryResponse(@Nullable Question supplementaryQuestion) {
        this.question = supplementaryQuestion;
        this.feedback = null;
    }

    public SupplementaryResponse(@Nullable SupplementaryFeedbackDto supplementaryFeedback) {
        this.question = null;
        this.feedback = supplementaryFeedback;
    }
}
