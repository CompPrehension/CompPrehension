package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.NewInteractionAnswerData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.mappers.Mapper;

@Component
class CarriedAnswerMapper implements Mapper<ResponseData, NewInteractionAnswerData> {

    @Override
    public @NotNull NewInteractionAnswerData map(@NotNull ResponseData source) {
        return new NewInteractionAnswerData(
                source.getId(),
                source.getLeftAnswerObject().getAnswerId(),
                source.getRightAnswerObject().getAnswerId(),
                source.getCreatedByInteractionId());
    }
}
