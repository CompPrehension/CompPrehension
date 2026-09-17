package org.vstu.compprehension.frontend.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.HyperText;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.data.question.AnswerFeedbackData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.QuestionType;
import org.vstu.compprehension.frontend.dto.AnswerDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackViolationLawDto;
import org.vstu.compprehension.frontend.dto.feedback.OrderQuestionFeedbackDto;
import org.vstu.compprehension.mappers.Mapper;

import java.util.List;

@Component
@RequiredArgsConstructor
class FeedbackDtoMapperImpl implements FeedbackDtoMapper {

    private final DomainFactory domainFactory;
    private final Mapper<ResponseData, AnswerDto> answerDtoMapper;

    @Override
    public @NotNull FeedbackDto map(@NotNull AnswerFeedbackData feedback, @NotNull Language language) {
        QuestionData question = feedback.question();
        FeedbackDto.FeedbackDtoBuilder<?, ?> builder = question.getContent().getQuestionType() == QuestionType.ORDER
                ? OrderQuestionFeedbackDto.builder().trace(getSolutionTrace(question, language))
                : FeedbackDto.builder();
        return builder
                .isCorrect(feedback.correct())
                .grade(feedback.grade())
                .correctSteps(question.correctInteractionsCount())
                .stepsWithErrors(question.erroneousInteractionsCount())
                .stepsLeft(feedback.stepsLeft())
                .correctAnswers(toAnswerDtos(feedback.correctAnswers()))
                .messages(toMessageDtos(feedback.messages()))
                .strategyDecision(feedback.strategyDecision())
                .build();
    }

    private @Nullable AnswerDto[] toAnswerDtos(@Nullable List<ResponseData> responses) {
        return responses == null ? null : responses.stream().map(answerDtoMapper::map).toArray(AnswerDto[]::new);
    }

    private @Nullable FeedbackDto.Message[] toMessageDtos(@Nullable List<AnswerFeedbackData.Message> messages) {
        return messages == null ? null : messages.stream().map(this::map).toArray(FeedbackDto.Message[]::new);
    }

    private @NotNull FeedbackDto.Message map(@NotNull AnswerFeedbackData.Message source) {
        List<FeedbackViolationLawDto> laws = source.laws() == null ? null
                : source.laws().stream().map(this::map).toList();
        return new FeedbackDto.Message(FeedbackDto.MessageType.valueOf(source.type().name()), source.text(), laws);
    }

    private @NotNull FeedbackViolationLawDto map(@NotNull AnswerFeedbackData.Law source) {
        return new FeedbackViolationLawDto(source.name(), source.canCreateSupplementaryQuestion());
    }

    private @NotNull String[] getSolutionTrace(@NotNull QuestionData question, @NotNull Language language) {
        return domainFactory.getDomain(question.getContent().getDomainId())
                .getFullSolutionTrace(question, language).stream()
                .map(HyperText::getText)
                .toArray(String[]::new);
    }
}
