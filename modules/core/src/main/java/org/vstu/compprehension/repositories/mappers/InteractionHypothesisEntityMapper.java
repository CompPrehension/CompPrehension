package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.AnswerHypothesisData;
import org.vstu.compprehension.entities.InteractionHypothesisEntity;
import org.vstu.compprehension.mappers.Mapper;

@Component
class InteractionHypothesisEntityMapper implements Mapper<AnswerHypothesisData, InteractionHypothesisEntity> {

    @Override
    public @NotNull InteractionHypothesisEntity map(@NotNull AnswerHypothesisData source) {
        var entity = new InteractionHypothesisEntity();
        entity.setName(source.name());
        entity.setCorrect(source.isCorrect());
        return entity;
    }
}
