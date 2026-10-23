package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.NewInteractionAnswerData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.data.question.SubmittedAnswerData;
import org.vstu.compprehension.mappers.Mapper;

@Component
class CarriedAnswerMapper implements Mapper<ResponseData, NewInteractionAnswerData> {

    @Override
    public @NotNull NewInteractionAnswerData map(@NotNull ResponseData source) {
        return new NewInteractionAnswerData(source.getId(), toSubmittedAnswer(source));
    }

    private @NotNull SubmittedAnswerData toSubmittedAnswer(@NotNull ResponseData source) {
        var leftAnswerId = source.getAnswer().left().getAnswerId();
        return switch (source.getAnswer()) {
            case AnswerData.Pair pair -> new SubmittedAnswerData.Pair(
                    leftAnswerId, pair.right().getAnswerId(), source.getCreatedByInteractionId());
            case AnswerData.Choice choice -> new SubmittedAnswerData.Choice(
                    leftAnswerId, choice.value(), source.getCreatedByInteractionId());
        };
    }
}
