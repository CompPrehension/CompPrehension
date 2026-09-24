package org.vstu.compprehension.repositories.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.CorrectLawData;
import org.vstu.compprehension.data.question.FeedbackData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.mappers.Mapper;

@Component
@RequiredArgsConstructor
class QuestionInteractionMapper implements Mapper<InteractionEntity, QuestionInteractionData> {

    private final ViolationMapper violationMapper;
    private final ResponseMapper responseMapper;

    @Override
    public @NotNull QuestionInteractionData map(@NotNull InteractionEntity source) {
        boolean hasViolations = !source.getViolations().isEmpty();
        return QuestionInteractionData.builder()
                .id(source.getId())
                .interactionType(source.getInteractionType())
                .feedback(source.getFeedback() == null ? null
                        : new FeedbackData(source.getFeedback().getId(), source.getFeedback().getGrade(),
                                source.getFeedback().getInteractionsLeft()))
                .violations(source.getViolations().stream()
                        .map(violation -> violationMapper.map(violation, source))
                        .toList())
                .correctLaw(source.getCorrectLaw().stream()
                        .map(law -> new CorrectLawData(law.getId(), law.getLawName()))
                        .toList())
                .responses(source.getResponses().stream()
                        .map(response -> responseMapper.map(response, hasViolations))
                        .toList())
                .build();
    }
}
