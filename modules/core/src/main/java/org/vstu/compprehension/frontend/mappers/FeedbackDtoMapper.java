package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.question.AnswerFeedbackData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.mappers.Mapping;

public interface FeedbackDtoMapper extends Mapping {

    @NotNull FeedbackDto map(@NotNull AnswerFeedbackData feedback, @NotNull Language language);
}
