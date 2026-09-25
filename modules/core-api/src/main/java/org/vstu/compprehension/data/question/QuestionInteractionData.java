package org.vstu.compprehension.data.question;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.InteractionType;

import java.util.List;

@Value
public class QuestionInteractionData {
    Long id;
    InteractionType interactionType;
    @Nullable FeedbackData feedback;
    @NotNull List<ViolationData> violations;
    @NotNull List<ResponseData> responses;
    @NotNull List<CorrectLawData> correctLaw;
    @NotNull List<AnswerData> answers;

    @Builder(toBuilder = true)
    public QuestionInteractionData(Long id,
                                   InteractionType interactionType,
                                   @Nullable FeedbackData feedback,
                                   @Nullable List<ViolationData> violations,
                                   @Nullable List<ResponseData> responses,
                                   @Nullable List<CorrectLawData> correctLaw) {
        this.id = id;
        this.interactionType = interactionType;
        this.feedback = feedback;
        this.violations = violations == null ? List.of() : List.copyOf(violations);
        this.responses = responses == null ? List.of() : List.copyOf(responses);
        this.correctLaw = correctLaw == null ? List.of() : List.copyOf(correctLaw);
        this.answers = this.responses.stream().map(ResponseData::getAnswer).toList();
    }

    public boolean isCorrect() {
        return violations.isEmpty();
    }

    public boolean allowsMoreSteps() {
        return feedback != null && feedback.getInteractionsLeft() >= 0;
    }
}
