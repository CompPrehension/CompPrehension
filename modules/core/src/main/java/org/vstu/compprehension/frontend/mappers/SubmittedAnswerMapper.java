package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.NewInteractionAnswerData;
import org.vstu.compprehension.data.question.SubmittedAnswerData;
import org.vstu.compprehension.mappers.Mapper;

@Component
class SubmittedAnswerMapper implements Mapper<SubmittedAnswerData, NewInteractionAnswerData> {

    @Override
    public @NotNull NewInteractionAnswerData map(@NotNull SubmittedAnswerData source) {
        return new NewInteractionAnswerData(
                null,
                source.leftAnswerId(),
                source.rightAnswerId(),
                source.createdByInteractionId());
    }
}
