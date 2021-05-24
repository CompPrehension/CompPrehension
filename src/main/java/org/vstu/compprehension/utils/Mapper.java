package org.vstu.compprehension.utils;

import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.vstu.compprehension.dto.*;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.dto.feedback.OrderQuestionFeedbackDto;
import org.vstu.compprehension.dto.question.MatchingQuestionDto;
import org.vstu.compprehension.dto.question.OrderQuestionDto;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class Mapper {

    public static UserInfoDto toDto(UserEntity user) {
        val displayName = Stream.of(user.getFirstName(), user.getLastName())
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(" "));
        return UserInfoDto.builder()
                .id(user.getId())
                .displayName(displayName)
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();
    }

    public static CorrectAnswerDto toDto(Domain.CorrectAnswer correctAnswer) {
        val frontAnswers = Optional.ofNullable(correctAnswer.answers).stream()
                .flatMap(Collection::stream)
                .map(pair -> List.of((long)pair.getLeft().getAnswerId(), (long)pair.getRight().getAnswerId()).toArray(new Long[2])).toArray(Long[][]::new);
        return CorrectAnswerDto.builder()
                .explanation(correctAnswer.explanation.getText())
                .answers(frontAnswers)
                .build();
    }

    public static QuestionDto toDto(Question questionObject) {
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
                .map(r -> Pair.of(r.getLeftAnswerObject(), r.getRightAnswerObject()))
                .map(pair -> List.of((long)pair.getLeft().getAnswerId(), (long)pair.getRight().getAnswerId()).toArray(new Long[2]))
                .toArray(Long[][]::new);

        val feedback = lastInteraction
                .map(i -> Mapper.toFeedbackDto(questionObject, i, null, correctInteractionsCount, interactionsWithErrorsCount, null))
                .orElse(null);

        val answers = question.getAnswerObjects() != null ? question.getAnswerObjects() : new ArrayList<AnswerObjectEntity>(0);
        val answerDtos = answers.stream()
                .map(a -> new QuestionAnswerDto((long)a.getAnswerId(), a.getHyperText()))
                .toArray(QuestionAnswerDto[]::new);
        switch (question.getQuestionType()) {
            case ORDER:
                val domain = DomainAdapter.getDomain(question.getDomainEntity().getName());
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

    public static ExerciseAttemptDto toDto(ExerciseAttemptEntity attempt) {
        val questionIds = Optional.ofNullable(attempt.getQuestions()).stream()
                .flatMap(Collection::stream)
                .map(QuestionEntity::getId)
                .toArray(Long[]::new);
        return ExerciseAttemptDto.builder()
                .exerciseId(attempt.getExercise().getId())
                .attemptId(attempt.getId())
                .questionIds(questionIds)
                .build();
    }

    public static FeedbackDto toFeedbackDto(
            Question question,
            InteractionEntity interaction,
            FeedbackDto.Message message,
            Integer correctSteps,
            Integer stepsWithErrors,
            Long[][] correctAnswers
    ) {
        if (interaction.getQuestion().getQuestionType() == QuestionType.ORDER) {
            val domain = DomainAdapter.getDomain(question.getQuestionData().getDomainEntity().getName());
            val trace = Optional.ofNullable(domain.getFullSolutionTrace(question)).stream()
                    .flatMap(Collection::stream)
                    .map(HyperText::getText)
                    .toArray(String[]::new);
            return OrderQuestionFeedbackDto.builder()
                    .correctSteps(correctSteps)
                    .stepsWithErrors(stepsWithErrors)
                    .grade(interaction.getFeedback().getGrade())
                    .message(message)
                    .correctAnswers(correctAnswers)
                    .stepsLeft(interaction.getFeedback().getInteractionsLeft())
                    .violations(interaction.getViolations().stream().map(ViolationEntity::getId).toArray(Long[]::new))
                    .trace(trace)
                    .build();
        }
        return FeedbackDto.builder()
                .correctSteps(correctSteps)
                .stepsWithErrors(stepsWithErrors)
                .grade(interaction.getFeedback().getGrade())
                .message(message)
                .correctAnswers(correctAnswers)
                .stepsLeft(interaction.getFeedback().getInteractionsLeft())
                .violations(interaction.getViolations().stream().map(ViolationEntity::getId).toArray(Long[]::new))
                .build();
    }
}
