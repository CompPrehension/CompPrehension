package org.vstu.compprehension.repositories.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.entities.AnswerObjectEntity;
import org.vstu.compprehension.entities.ResponseEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.utils.Strict;

@Component
@RequiredArgsConstructor
class ResponseMapperImpl implements ResponseMapper {

    private final Mapper<AnswerObjectEntity, AnswerObjectData> answerObjectMapper;

    @Override
    public @NotNull ResponseData map(@NotNull ResponseEntity response, boolean interactionHasViolations) {
        var createdBy = response.getCreatedByInteraction();
        var owner = "response " + response.getId();
        return new ResponseData(
                response.getId(),
                response.getSpecValue(),
                answerObjectMapper.map(Strict.required(
                        response.getLeftAnswerObject(), "leftAnswerObject", owner)),
                answerObjectMapper.map(Strict.required(
                        response.getRightAnswerObject(), "rightAnswerObject", owner)),
                createdBy == null ? null : createdBy.getInteractionType(),
                createdBy == null ? null : createdBy.getId(),
                interactionHasViolations);
    }
}
