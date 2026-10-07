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
    boolean isCorrect;
    @NotNull List<InteractionReasoningData> reasonings;
    @NotNull List<ResponseData> responses;
    @NotNull List<AnswerData> answers;
    @Nullable InteractionClarificationData clarification;
    @NotNull List<ViolationData> violations;
    @NotNull List<String> appliedKnowledge;

    @Builder(toBuilder = true)
    public QuestionInteractionData(Long id,
                                   InteractionType interactionType,
                                   @Nullable FeedbackData feedback,
                                   boolean isCorrect,
                                   @Nullable List<InteractionReasoningData> reasonings,
                                   @Nullable List<ResponseData> responses,
                                   @Nullable InteractionClarificationData clarification) {
        this.id = id;
        this.interactionType = interactionType;
        this.feedback = feedback;
        this.isCorrect = isCorrect;
        this.reasonings = reasonings == null ? List.of() : List.copyOf(reasonings);
        this.responses = responses == null ? List.of() : List.copyOf(responses);
        this.answers = this.responses.stream().map(ResponseData::getAnswer).toList();
        this.clarification = clarification;
        var counted = new CountedKnowledgeData(isCorrect, this.reasonings,
                clarification == null ? null : clarification.chosenReasoning());
        this.violations = counted.getViolations();
        this.appliedKnowledge = counted.getAppliedKnowledge();
    }

    public boolean allowsMoreSteps() {
        return feedback != null && feedback.getInteractionsLeft() >= 0;
    }
}
