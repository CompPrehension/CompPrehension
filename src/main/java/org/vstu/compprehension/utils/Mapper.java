package org.vstu.compprehension.utils;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.dto.*;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.dto.feedback.OrderQuestionFeedbackDto;
import org.vstu.compprehension.dto.question.MatchingQuestionDto;
import org.vstu.compprehension.dto.question.OrderQuestionDto;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.EnumData.InteractionType;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Mapper {

    public static @NotNull UserInfoDto toDto(@NotNull UserEntity user) {
        val displayName = Stream.of(user.getFirstName(), user.getLastName())
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(" "));
        return UserInfoDto.builder()
                .id(user.getId())
                .displayName(displayName)
                .email(user.getEmail())
                .roles(user.getRoles())
                .preferredLanguage(user.getPreferred_language())
                .build();
    }

    public static @NotNull AnswerDto toDto(@NotNull ResponseEntity response) {
        return AnswerDto.builder()
                .isCreatedByUser(response.getCreatedByInteraction().getInteractionType() == InteractionType.SEND_RESPONSE)
                .createdByInteraction(response.getCreatedByInteraction().getId())
                .answer(new Long[] { (long)response.getLeftAnswerObject().getAnswerId(), (long)response.getRightAnswerObject().getAnswerId() })
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

    public static @NotNull QuestionDto toDto(@NotNull Question questionObject) {
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
                .map(Mapper::toDto)
                .toArray(AnswerDto[]::new);

        val feedback = lastInteraction
                .map(i -> Mapper.toFeedbackDto(questionObject, null, correctInteractionsCount, interactionsWithErrorsCount, i.getFeedback().getGrade(), i.getFeedback().getInteractionsLeft(), null, i.getViolations().size() == 0, null))
                .orElse(null);

        val answers = question.getAnswerObjects() != null ? question.getAnswerObjects() : new ArrayList<AnswerObjectEntity>(0);
        val answerDtos = answers.stream()
                .map(a -> new QuestionAnswerDto((long)a.getAnswerId(), a.getHyperText()))
                .toArray(QuestionAnswerDto[]::new);
        switch (question.getQuestionType()) {
            case ORDER:
                val domain = DomainAdapter.getDomain(question.getDomainEntity().getClassPath());
                val trace = Optional.ofNullable(domain.getFullSolutionTrace(questionObject)).stream()
                        .flatMap(Collection::stream)
                        .map(HyperText::getText)
                        .toArray(String[]::new);
                return OrderQuestionDto.builder()
                        .attemptId(question.getExerciseAttempt().getId())
                        .questionId(question.getId())
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
                        .attemptId(question.getExerciseAttempt().getId())
                        .questionId(question.getId())
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
                        .attemptId(question.getExerciseAttempt().getId())
                        .questionId(question.getId())
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

    public static @NotNull ExerciseAttemptDto toDto(@NotNull ExerciseAttemptEntity attempt) {
        val questionIds = Optional.ofNullable(attempt.getQuestions()).stream()
                .flatMap(Collection::stream)
                .filter(q -> !q.getQuestionDomainType().contains("Supplementary"))
                .map(QuestionEntity::getId)
                .toArray(Long[]::new);
        return ExerciseAttemptDto.builder()
                .exerciseId(attempt.getExercise().getId())
                .attemptId(attempt.getId())
                .questionIds(questionIds)
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
            @Nullable Decision strategyDecision
    ) {
        if (question.getQuestionData().getQuestionType() == QuestionType.ORDER) {
            val domain = DomainAdapter.getDomain(question.getQuestionData().getDomainEntity().getClassPath());
            val trace = Optional.ofNullable(domain.getFullSolutionTrace(question)).stream()
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
}
