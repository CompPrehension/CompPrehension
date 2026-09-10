package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.AnswerDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.mappers.Mapping;

public interface FeedbackDtoMapper extends Mapping {

    @NotNull FeedbackDto map(@NotNull QuestionData question,
                             @Nullable FeedbackDto.Message[] messages,
                             @Nullable Integer correctSteps,
                             @Nullable Integer stepsWithErrors,
                             @Nullable Float grade,
                             @Nullable Integer stepsLeft,
                             @Nullable AnswerDto[] correctAnswers,
                             boolean isCorrect,
                             @Nullable Decision strategyDecision,
                             @NotNull Language language);
}
