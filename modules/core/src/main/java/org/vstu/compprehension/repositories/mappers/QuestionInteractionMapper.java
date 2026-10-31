package org.vstu.compprehension.repositories.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.FeedbackData;
import org.vstu.compprehension.data.question.InteractionClarificationData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.mappers.Mapper;

@Component
@RequiredArgsConstructor
class QuestionInteractionMapper implements Mapper<InteractionEntity, QuestionInteractionData> {

    private final ResponseMapper responseMapper;

    @Override
    public @NotNull QuestionInteractionData map(@NotNull InteractionEntity source) {
        return QuestionInteractionData.builder()
                .id(source.getId())
                .interactionType(source.getInteractionType())
                .feedback(source.getFeedback() == null ? null
                        : new FeedbackData(source.getFeedback().getId(), source.getFeedback().getGrade(),
                                source.getFeedback().getInteractionsLeft()))
                .isCorrect(source.isCorrect())
                .reasonings(source.getReasonings())
                .responses(source.getResponses().stream()
                        .map(response -> responseMapper.map(response, !source.isCorrect()))
                        .toList())
                .clarification(source.getClarification() == null ? null
                        : new InteractionClarificationData(source.getClarification().getContent(),
                                source.getClarification().getAnsweredAt() != null,
                                source.getClarification().getChosenReasoning()))
                .build();
    }
}
