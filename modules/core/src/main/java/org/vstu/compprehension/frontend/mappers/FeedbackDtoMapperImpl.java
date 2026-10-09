package org.vstu.compprehension.frontend.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.HyperText;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.data.question.AnswerFeedbackData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.AnswerDto;
import org.vstu.compprehension.frontend.dto.feedback.ClarificationDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackKnowledgeDto;
import org.vstu.compprehension.mappers.Mapper;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
class FeedbackDtoMapperImpl implements FeedbackDtoMapper {

    private final DomainFactory domainFactory;
    private final Mapper<ResponseData, AnswerDto> answerDtoMapper;

    @Override
    public @NotNull FeedbackDto map(@NotNull AnswerFeedbackData feedback, @NotNull Language language) {
        QuestionData question = feedback.question();
        return FeedbackDto.builder()
                .isCorrect(feedback.correct())
                .grade(feedback.grade())
                .correctSteps(question.correctInteractionsCount())
                .stepsWithErrors(question.erroneousInteractionsCount())
                .stepsLeft(feedback.stepsLeft())
                .correctAnswers(toAnswerDtos(feedback.correctAnswers()))
                .messages(toMessageDtos(feedback.messages()))
                .strategyDecision(feedback.strategyDecision())
                .clarification(question.findInteractionAwaitingClarification().map(this::toClarificationDto).orElse(null))
                .trace(question.getContent().getOptions().isShowTrace() ? getSolutionTrace(question, language) : null)
                .build();
    }

    private @NotNull ClarificationDto toClarificationDto(@NotNull QuestionInteractionData interaction) {
        var content = Objects.requireNonNull(interaction.getClarification()).content();
        var reasonings = interaction.getReasonings();
        return new ClarificationDto(content.prompt(), content.options().stream()
                .map(option -> new ClarificationDto.Option(option.reasoning(),
                        Objects.requireNonNull(reasonings.stream()
                                .filter(reasoning -> reasoning.getId() == option.reasoning())
                                .findFirst().orElseThrow().getReason())))
                .toList());
    }

    private @Nullable AnswerDto[] toAnswerDtos(@Nullable List<ResponseData> responses) {
        return responses == null ? null : responses.stream().map(answerDtoMapper::map).toArray(AnswerDto[]::new);
    }

    private @Nullable FeedbackDto.Message[] toMessageDtos(@Nullable List<AnswerFeedbackData.Message> messages) {
        return messages == null ? null : messages.stream().map(this::map).toArray(FeedbackDto.Message[]::new);
    }

    private @NotNull FeedbackDto.Message map(@NotNull AnswerFeedbackData.Message source) {
        List<FeedbackKnowledgeDto> laws = source.knowledge() == null ? null
                : source.knowledge().stream().map(this::map).toList();
        return new FeedbackDto.Message(FeedbackDto.MessageType.valueOf(source.type().name()), source.text(), laws);
    }

    private @NotNull FeedbackKnowledgeDto map(@NotNull AnswerFeedbackData.Knowledge source) {
        return new FeedbackKnowledgeDto(source.name(), source.canCreateSupplementaryQuestion());
    }

    private @NotNull String[] getSolutionTrace(@NotNull QuestionData question, @NotNull Language language) {
        return domainFactory.getDomain(question.getContent().getDomainId())
                .getFullSolutionTrace(question, language).stream()
                .map(HyperText::getText)
                .toArray(String[]::new);
    }
}
