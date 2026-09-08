package org.vstu.compprehension.frontend.mappers;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.HyperText;
import org.vstu.compprehension.data.exerciseattempt.AttemptSummaryData;
import org.vstu.compprehension.data.survey.SurveyData;
import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.frontend.dto.*;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.frontend.dto.feedback.OrderQuestionFeedbackDto;
import org.vstu.compprehension.frontend.dto.question.MatchingQuestionDto;
import org.vstu.compprehension.frontend.dto.question.OrderQuestionDto;
import org.vstu.compprehension.frontend.dto.question.QuestionDto;
import org.vstu.compprehension.frontend.dto.survey.SurveyDto;
import org.vstu.compprehension.frontend.dto.survey.SurveyQuestionDto;
import org.vstu.compprehension.businesslogic.Question;
import org.vstu.compprehension.businesslogic.SupplementaryResponse;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.QuestionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Старый god-класс Data → DTO: статические перегрузки {@code toDto} на все случаи сразу.
 * <p>
 * Нарушает соглашения из {@code package-info} этого пакета почти целиком: статика вместо
 * бина, перегрузки вместо отдельных мапперов, второй аргумент-контекст, обращение к
 * доменной логике из маппинга. Разбирается на отдельные мапперы; новых методов здесь
 * не появляется.
 *
 * @deprecated разбирается на классы, реализующие {@link Mapper}.
 */
@Deprecated
public class LegacyDtoMappers {

    public static @NotNull SurveyDto toDto(@NotNull SurveyData survey) {
        return SurveyDto.builder()
                .surveyId(survey.surveyId())
                .options(survey.options())
                .questions(survey.questions().stream()
                        .map(q -> SurveyQuestionDto.builder()
                                .id(q.id())
                                .type(q.type())
                                .text(q.text())
                                .required(q.required())
                                .policy(q.policy())
                                .options(q.options())
                                .build())
                        .toArray(SurveyQuestionDto[]::new))
                .build();
    }


    public static @NotNull UserInfoDto toDto(@NotNull UserData user, @NotNull UserPermissionsDto permissions) {
        val displayName = Stream.of(user.firstName(), user.lastName())
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(" "));
        return UserInfoDto.builder()
                .id(user.id())
                .displayName(displayName)
                .email(user.email())
                .language(user.language().toLocaleString())
                .permissions(permissions)
                .build();
    }

    /** Ответ студента в виде данных — так их отдаёт вопрос после перехода на QuestionData. */
    public static @NotNull AnswerDto toDto(@NotNull ResponseData response) {
        return AnswerDto.builder()
                .isCreatedByUser(response.getCreatedByInteractionType() == InteractionType.SEND_RESPONSE)
                .createdByInteraction(response.getCreatedByInteractionId())
                .answer(new Long[] { (long)response.getLeftAnswerObject().getAnswerId(),
                        (long)response.getRightAnswerObject().getAnswerId() })
                .build();
    }

    /*
    public static @NotNull CorrectAnswerDto toDto(@NotNull Domain.CorrectAnswer correctAnswer) {
        val frontAnswers = Optional.ofNullable(correctAnswer.answers).stream()
                .flatMap(Collection::stream)
                .map(answr -> new AnswerDto((long)answr.getLeft().getAnswerId(), (long)answr.getRight().getAnswerId(), answr.isCreatedByUser()))
                .toArray(AnswerDto[]::new);
        return CorrectAnswerDto.builder()
                .explanation(correctAnswer.explanation.getText())
                .answers(frontAnswers)
                .build();
    }
    */

    public static @NotNull QuestionDto toDto(@NotNull Question questionObject, Language language) {
        val question = questionObject.getQuestionData();

        // calculate last interaction responses
        val totalInteractionsCount = Optional.ofNullable(question.getInteractions())
                .map(is -> is.size()).orElse(0);
        val interactionsWithErrorsCount = (int)Optional.ofNullable(question.getInteractions()).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getViolations().size() > 0).count();
        val correctInteractionsCount = (int)Optional.ofNullable(question.getInteractions()).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getCorrectLaw().size() > 0).count();
        val lastCorrectInteraction = Optional.ofNullable(question.getInteractions()).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getViolations().size() == 0) // select only interactions without mistakes
                .reduce((first, second) -> second);
        val lastInteraction = Optional.ofNullable(question.getInteractions()).stream()
                .flatMap(Collection::stream)
                .reduce((first, second) -> second);
        val responses = lastCorrectInteraction
                .flatMap(i -> Optional.ofNullable(i.getResponses())).stream()
                .flatMap(Collection::stream)
                .map(LegacyDtoMappers::toDto)
                .toArray(AnswerDto[]::new);

        val feedback = lastInteraction
                .map(i -> LegacyDtoMappers.toFeedbackDto(questionObject, null, correctInteractionsCount, interactionsWithErrorsCount, i.getFeedback().getGrade(), i.getFeedback().getInteractionsLeft(), null, i.getViolations().size() == 0, null, language))
                .orElse(null);

        val answers = question.getAnswerObjects() != null ? question.getAnswerObjects() : new ArrayList<AnswerObjectData>(0);
        val answerDtos = answers.stream()
                .map(a -> new QuestionAnswerDto((long)a.getAnswerId(), a.getHyperText()))
                .toArray(QuestionAnswerDto[]::new);
        switch (question.getQuestionType()) {
            case ORDER:
                val trace = Optional.ofNullable(questionObject.getDomain())
                        .map(d -> d.getFullSolutionTrace(questionObject, language)).stream()
                        .flatMap(Collection::stream)
                        .map(HyperText::getText)
                        .toArray(String[]::new);
                return OrderQuestionDto.builder()
                        .questionId(question.getId())
                        .questionMetadataId(question.getMetadata() == null ? -1: question.getMetadata().getId())
                        .type(question.getQuestionType().toString())
                        .answers(answerDtos)
                        .text(question.getQuestionText())
                        .options(question.getOptions())
                        .responses(responses)
                        .feedback(feedback)
                        .initialTrace(trace)
                        .build();
            case MULTI_CHOICE:
            case SINGLE_CHOICE:
                return QuestionDto.builder()
                        .questionId(question.getId())
                        .questionMetadataId(question.getMetadata() == null ? -1: question.getMetadata().getId())
                        .type(question.getQuestionType().toString())
                        .answers(answerDtos)
                        .text(question.getQuestionText())
                        .options(question.getOptions())
                        .responses(responses)
                        .feedback(feedback)
                        .build();
            case MATCHING:
                QuestionAnswerDto[] left = IntStream.range(0, answers.size())
                        .filter(i -> !answers.get(i).isRightCol())
                        .mapToObj(i -> new QuestionAnswerDto((long)answers.get(i).getAnswerId(), answers.get(i).getHyperText()))
                        .toArray(QuestionAnswerDto[]::new);
                QuestionAnswerDto[] right = IntStream.range(0, answers.size())
                        .filter(i -> answers.get(i).isRightCol())
                        .mapToObj(i -> new QuestionAnswerDto((long)answers.get(i).getAnswerId(), answers.get(i).getHyperText()))
                        .toArray(QuestionAnswerDto[]::new);

                return MatchingQuestionDto.builder()
                        .questionId(question.getId())
                        .questionMetadataId(question.getMetadata() == null ? -1: question.getMetadata().getId())
                        .type(question.getQuestionType().toString())
                        .answers(left)
                        .groups(right)
                        .text(question.getQuestionText())
                        .options(question.getOptions())
                        .responses(responses)
                        .feedback(feedback)
                        .build();
            default:
                throw new UnsupportedOperationException("Invalid mapping");
        }
    }

    public static @NotNull ExerciseAttemptDto toDto(@NotNull AttemptSummaryData attempt) {
        return ExerciseAttemptDto.builder()
                .userId(attempt.userId())
                .exerciseId(attempt.exerciseId())
                .courseId(attempt.courseId())
                .attemptId(attempt.attemptId())
                .questionIds(attempt.questionIds().toArray(Long[]::new))
                .status(attempt.status())
                .build();
    }

    public static @NotNull FeedbackDto toFeedbackDto(
            @NotNull Question question,
            @Nullable FeedbackDto.Message[] messages,
            @Nullable Integer correctSteps,
            @Nullable Integer stepsWithErrors,
            @Nullable Float grade,
            @Nullable Integer interactionsLeft,
            @Nullable AnswerDto[] correctAnswers,
            boolean isCorrect,
            @Nullable Decision strategyDecision,
            @NotNull Language language
    ) {
        if (question.getQuestionData().getQuestionType() == QuestionType.ORDER) {
            val trace = Optional.ofNullable(question.getDomain())
                    .map(d -> d.getFullSolutionTrace(question, language)).stream()
                    .flatMap(Collection::stream)
                    .map(HyperText::getText)
                    .toArray(String[]::new);
            return OrderQuestionFeedbackDto.builder()
                    .correctSteps(correctSteps)
                    .stepsWithErrors(stepsWithErrors)
                    .grade(grade)
                    .messages(messages)
                    .correctAnswers(correctAnswers)
                    .stepsLeft(interactionsLeft)
                    .strategyDecision(strategyDecision)
                    .trace(trace)
                    .isCorrect(isCorrect)
                    .build();
        }
        return FeedbackDto.builder()
                .correctSteps(correctSteps)
                .stepsWithErrors(stepsWithErrors)
                .grade(grade)
                .messages(messages)
                .correctAnswers(correctAnswers)
                .stepsLeft(interactionsLeft)
                .strategyDecision(strategyDecision)
                .isCorrect(isCorrect)
                .build();
    }


    public static @NotNull SupplementaryQuestionDto toDto(@NotNull SupplementaryResponse response, @NotNull Language language) {
        if(response.getQuestion() != null) {
            QuestionDto questionDto = LegacyDtoMappers.toDto(response.getQuestion(), language);
            return questionDto.getAnswers().length > 0 ? SupplementaryQuestionDto.FromQuestion(questionDto)
                    : SupplementaryQuestionDto.FromMessage(new SupplementaryFeedbackDto(FeedbackDto.Message.Success(questionDto.getText().replaceAll("<[^>]*>", "")), SupplementaryFeedbackDto.Action.Finish));
        }
        else
            return SupplementaryQuestionDto.FromMessage(response.getFeedback());
    }
}
