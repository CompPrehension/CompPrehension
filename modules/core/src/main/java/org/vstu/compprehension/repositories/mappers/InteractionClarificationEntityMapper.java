package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.HypothesisClarificationData;
import org.vstu.compprehension.entities.InteractionClarificationEntity;
import org.vstu.compprehension.mappers.Mapper;

@Component
class InteractionClarificationEntityMapper implements Mapper<HypothesisClarificationData, InteractionClarificationEntity> {

    @Override
    public @NotNull InteractionClarificationEntity map(@NotNull HypothesisClarificationData source) {
        var entity = new InteractionClarificationEntity();
        entity.setContent(source);
        return entity;
    }
}
