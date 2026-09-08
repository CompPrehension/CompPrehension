package org.vstu.compprehension.repositories.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.InteractionResponsesData;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.mappers.Mapper;

@Component
@RequiredArgsConstructor
class InteractionResponsesMapper implements Mapper<InteractionEntity, InteractionResponsesData> {

    private final ResponseMapper responseMapper;

    @Override
    public @NotNull InteractionResponsesData map(@NotNull InteractionEntity source) {
        boolean hasViolations = !source.getViolations().isEmpty();
        return new InteractionResponsesData(
                source.getId(),
                source.getResponses().stream()
                        .map(response -> responseMapper.map(response, hasViolations))
                        .toList());
    }
}
