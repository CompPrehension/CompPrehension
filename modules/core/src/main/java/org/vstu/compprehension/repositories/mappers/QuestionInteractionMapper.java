package org.vstu.compprehension.repositories.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.CorrectLawData;
import org.vstu.compprehension.data.question.FeedbackData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.mappers.Mapper;

import java.util.ArrayList;
import java.util.stream.Collectors;

/** Взаимодействие целиком: нарушения, ответы и подтверждённые законы обязаны быть подгружены. */
@Component
@RequiredArgsConstructor
class QuestionInteractionMapper implements Mapper<InteractionEntity, QuestionInteractionData> {

    private final ViolationMapper violationMapper;
    private final ResponseMapper responseMapper;

    @Override
    public @NotNull QuestionInteractionData map(@NotNull InteractionEntity source) {
        var data = new QuestionInteractionData();
        data.setId(source.getId());
        data.setInteractionType(source.getInteractionType());
        // Оценка допускает отсутствие: связь объявлена с @NotFound(IGNORE), и на висячую
        // ссылку Hibernate подставляет null вместо ошибки.
        data.setFeedback(source.getFeedback() == null ? null
                : new FeedbackData(source.getFeedback().getId(), source.getFeedback().getGrade(),
                        source.getFeedback().getInteractionsLeft()));
        data.setViolations(source.getViolations().stream()
                .map(violation -> violationMapper.map(violation, source))
                .collect(Collectors.toCollection(ArrayList::new)));
        data.setCorrectLaw(source.getCorrectLaw().stream()
                .map(law -> new CorrectLawData(law.getId(), law.getLawName()))
                .collect(Collectors.toCollection(ArrayList::new)));

        boolean hasViolations = !source.getViolations().isEmpty();
        data.setResponses(source.getResponses().stream()
                .map(response -> responseMapper.map(response, hasViolations))
                .collect(Collectors.toCollection(ArrayList::new)));
        return data;
    }
}
