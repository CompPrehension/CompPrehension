package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.HyperText;
import org.vstu.compprehension.businesslogic.Question;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.QuestionType;
import org.vstu.compprehension.frontend.dto.AnswerDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.frontend.dto.feedback.OrderQuestionFeedbackDto;

import java.util.Collection;
import java.util.Optional;

@Component
class FeedbackDtoMapperImpl implements FeedbackDtoMapper {

    @Override
    public @NotNull FeedbackDto map(@NotNull Question question,
                                    @Nullable FeedbackDto.Message[] messages,
                                    @Nullable Integer correctSteps,
                                    @Nullable Integer stepsWithErrors,
                                    @Nullable Float grade,
                                    @Nullable Integer stepsLeft,
                                    @Nullable AnswerDto[] correctAnswers,
                                    boolean isCorrect,
                                    @Nullable Decision strategyDecision,
                                    @NotNull Language language) {
        if (question.getQuestionData().getQuestionType() == QuestionType.ORDER) {
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

    private @NotNull String[] getSolutionTrace(@NotNull Question question, @NotNull Language language) {
        return Optional.of(question.getDomain())
                .map(domain -> domain.getFullSolutionTrace(question, language)).stream()
                .flatMap(Collection::stream)
                .map(HyperText::getText)
                .toArray(String[]::new);
    }
}
