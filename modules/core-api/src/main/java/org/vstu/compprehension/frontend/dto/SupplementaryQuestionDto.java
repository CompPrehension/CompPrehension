package org.vstu.compprehension.frontend.dto;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.frontend.dto.question.QuestionDto;

@Getter @Setter
public class SupplementaryQuestionDto {
    private SupplementaryQuestionDto(@Nullable QuestionDto question, @Nullable SupplementaryFeedbackDto message) {
        this.question = question;
        this.message  = message;
    }
    private @Nullable QuestionDto question;
    private @Nullable SupplementaryFeedbackDto message;

    public static SupplementaryQuestionDto FromQuestion(@NotNull QuestionDto question) {
        return new SupplementaryQuestionDto(question, null);
    }
    public static SupplementaryQuestionDto FromMessage(@NotNull SupplementaryFeedbackDto message) {
        return new SupplementaryQuestionDto(null, message);
    }
    public static SupplementaryQuestionDto Empty() {
        return new SupplementaryQuestionDto(null, null);
    }
}
