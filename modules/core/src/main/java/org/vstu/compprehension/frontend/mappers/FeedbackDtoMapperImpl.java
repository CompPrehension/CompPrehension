package org.vstu.compprehension.frontend.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.HyperText;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.QuestionType;
import org.vstu.compprehension.frontend.dto.AnswerDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.frontend.dto.feedback.OrderQuestionFeedbackDto;


@Component
@RequiredArgsConstructor
class FeedbackDtoMapperImpl implements FeedbackDtoMapper {

    private final DomainFactory domainFactory;

    @Override
    public @NotNull FeedbackDto map(@NotNull QuestionData question,
                                    @Nullable FeedbackDto.Message[] messages,
                                    @Nullable Integer correctSteps,
                                    @Nullable Integer stepsWithErrors,
                                    @Nullable Float grade,
                                    @Nullable Integer stepsLeft,
                                    @Nullable AnswerDto[] correctAnswers,
                                    boolean isCorrect,
                                    @Nullable Decision strategyDecision,
                                    @NotNull Language language) {
        if (question.getContent().getQuestionType() == QuestionType.ORDER) {
            return OrderQuestionFeedbackDto.builder()
                    .correctSteps(correctSteps)
                    .stepsWithErrors(stepsWithErrors)
                    .grade(grade)
                    .messages(messages)
                    .correctAnswers(correctAnswers)
                    .stepsLeft(stepsLeft)
                    .strategyDecision(strategyDecision)
                    .trace(getSolutionTrace(question, language))
                    .isCorrect(isCorrect)
                    .build();
        }
        return FeedbackDto.builder()
                .correctSteps(correctSteps)
                .stepsWithErrors(stepsWithErrors)
                .grade(grade)
                .messages(messages)
                .correctAnswers(correctAnswers)
                .stepsLeft(stepsLeft)
                .strategyDecision(strategyDecision)
                .isCorrect(isCorrect)
                .build();
    }

    private @NotNull String[] getSolutionTrace(@NotNull QuestionData question, @NotNull Language language) {
        return domainFactory.getDomain(question.getContent().getDomainId())
                .getFullSolutionTrace(question, language).stream()
                .map(HyperText::getText)
                .toArray(String[]::new);
    }
}
