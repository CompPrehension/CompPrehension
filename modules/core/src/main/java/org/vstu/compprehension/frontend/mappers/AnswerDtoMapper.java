package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.frontend.dto.AnswerDto;
import org.vstu.compprehension.mappers.Mapper;

@Component
class AnswerDtoMapper implements Mapper<ResponseData, AnswerDto> {

    @Override
    public @NotNull AnswerDto map(@NotNull ResponseData source) {
        return AnswerDto.builder()
                .isCreatedByUser(source.getCreatedByInteractionType() == InteractionType.SEND_RESPONSE)
                .createdByInteraction(source.getCreatedByInteractionId())
                .answer(new Long[] {
                        (long) source.getLeftAnswerObject().getAnswerId(),
                        (long) source.getRightAnswerObject().getAnswerId() })
                .build();
    }
}
