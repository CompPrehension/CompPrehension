package org.vstu.compprehension.frontend.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.HyperText;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionContentData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.AnswerDto;
import org.vstu.compprehension.frontend.dto.QuestionAnswerDto;
import org.vstu.compprehension.frontend.dto.question.MatchingQuestionDto;
import org.vstu.compprehension.frontend.dto.question.OrderQuestionDto;
import org.vstu.compprehension.frontend.dto.question.QuestionDto;
import org.vstu.compprehension.mappers.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class QuestionDtoMapperImpl implements QuestionDtoMapper {

    private final Mapper<ResponseData, AnswerDto> answerDtoMapper;
    private final DomainFactory domainFactory;
    private final FeedbackDtoMapper feedbackDtoMapper;

    @Override
    public @NotNull QuestionDto map(@NotNull QuestionData question, @NotNull Language language) {
        QuestionContentData content = question.getContent();
        List<QuestionInteractionData> interactions = question.getInteractions();

        int stepsWithErrors = (int) interactions.stream()
                .filter(i -> !i.getViolations().isEmpty()).count();
        int correctSteps = (int) interactions.stream()
                .filter(i -> !i.getCorrectLaw().isEmpty()).count();

        var lastCorrect = question.latestCorrectInteraction();
        var last = interactions.stream().reduce((first, second) -> second);

        AnswerDto[] responses = lastCorrect
                .flatMap(i -> Optional.ofNullable(i.getResponses())).stream()
                .flatMap(Collection::stream)
                .map(answerDtoMapper::map)
                .toArray(AnswerDto[]::new);

        var feedback = last
                .map(i -> feedbackDtoMapper.map(question, null, correctSteps, stepsWithErrors,
                        i.getFeedback().getGrade(), i.getFeedback().getInteractionsLeft(), null,
                        i.getViolations().isEmpty(), null, language))
                .orElse(null);

        List<AnswerObjectData> answers = content.getAnswerObjects();
        Integer metadataId = content.getMetadata() == null ? -1 : content.getMetadata().getId();

        return switch (content.getQuestionType()) {
            case ORDER -> OrderQuestionDto.builder()
                    .questionId(question.getId())
                    .questionMetadataId(metadataId)
                    .type(content.getQuestionType().toString())
                    .answers(toAnswerDtos(answers))
                    .text(content.getQuestionText())
                    .options(content.getOptions())
                    .responses(responses)
                    .feedback(feedback)
                    .initialTrace(getSolutionTrace(question, language))
                    .build();
            case MULTI_CHOICE, SINGLE_CHOICE -> QuestionDto.builder()
                    .questionId(question.getId())
                    .questionMetadataId(metadataId)
                    .type(content.getQuestionType().toString())
                    .answers(toAnswerDtos(answers))
                    .text(content.getQuestionText())
                    .options(content.getOptions())
                    .responses(responses)
                    .feedback(feedback)
                    .build();
            case MATCHING -> MatchingQuestionDto.builder()
                    .questionId(question.getId())
                    .questionMetadataId(metadataId)
                    .type(content.getQuestionType().toString())
                    .answers(toAnswerDtos(answers.stream().filter(a -> !a.isRightCol()).toList()))
                    .groups(toAnswerDtos(answers.stream().filter(AnswerObjectData::isRightCol).toList()))
                    .text(content.getQuestionText())
                    .options(content.getOptions())
                    .responses(responses)
                    .feedback(feedback)
                    .build();
            default -> throw new UnsupportedOperationException(
                    "No DTO shape for question type " + content.getQuestionType());
        };
    }

    private QuestionAnswerDto[] toAnswerDtos(@NotNull List<AnswerObjectData> answers) {
        return answers.stream()
                .map(a -> new QuestionAnswerDto((long) a.getAnswerId(), a.getHyperText()))
                .toArray(QuestionAnswerDto[]::new);
    }

    private @NotNull String[] getSolutionTrace(@NotNull QuestionData question, @NotNull Language language) {
        return domainFactory.getDomain(question.getContent().getDomainId())
                .getFullSolutionTrace(question, language).stream()
                .map(HyperText::getText)
                .toArray(String[]::new);
    }
}
