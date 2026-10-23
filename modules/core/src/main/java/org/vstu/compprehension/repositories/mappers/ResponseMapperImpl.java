package org.vstu.compprehension.repositories.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.AnswerData;
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
                toAnswer(response, owner),
                createdBy == null ? null : createdBy.getInteractionType(),
                createdBy == null ? null : createdBy.getId(),
                interactionHasViolations);
    }

    private @NotNull AnswerData toAnswer(@NotNull ResponseEntity response, @NotNull String owner) {
        var left = answerObjectMapper.map(Strict.required(
                response.getLeftAnswerObject(), "leftAnswerObject", owner));
        var right = response.getRightAnswerObject();
        var value = response.getValue();
        if (right != null && value == null) {
            return new AnswerData.Pair(left, answerObjectMapper.map(right));
        }
        if (right == null && value != null) {
            return new AnswerData.Choice(left, value);
        }
        throw new IllegalStateException(
                "Exactly one of rightAnswerObject and value must be set for " + owner);
    }
}
