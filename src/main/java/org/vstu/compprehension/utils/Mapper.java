package org.vstu.compprehension.utils;

import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.vstu.compprehension.dto.FeedbackDto;
import org.vstu.compprehension.dto.QuestionAnswerDto;
import org.vstu.compprehension.dto.UserInfoDto;
import org.vstu.compprehension.dto.question.MatchingQuestionDto;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;

import javax.persistence.Tuple;
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
        return UserInfoDto.builder()
                .id(user.getId())
                .displayName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();
    }

    public static QuestionDto toDto(QuestionEntity question) {
        // calculate last interaction responses
        val totalInteractionsCount = Optional.ofNullable(question.getInteractions())
                .map(is -> is.size()).orElse(0);
        val interactionsWithErrorsCount = (int)Optional.ofNullable(question.getInteractions()).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getMistakes().size() > 0).count();
        val lastCorrectInteraction = Optional.ofNullable(question.getInteractions()).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getMistakes().size() == 0) // select only interactions without mistakes
                .reduce((first, second) -> second);
        val lastInteraction = Optional.ofNullable(question.getInteractions()).stream()
                .flatMap(Collection::stream)
                .reduce((first, second) -> second);
        val responses = lastCorrectInteraction
                .flatMap(i -> Optional.ofNullable(i.getResponses()))
                .map(resps -> {
                    val answers = question.getAnswerObjects();
                    return resps.stream()
                            .map(r -> Pair.of(r.getLeftAnswerObject(), r.getRightAnswerObject()))
                            .map(pair -> {
                                val leftIdx = LongStream.range(0, answers.size())
                                        .filter(i -> answers.get((int) i).getId().equals(pair.getLeft().getId()))
                                        .findFirst();
                                val rightIdx = LongStream.range(0, answers.size())
                                        .filter(i -> answers.get((int) i).getId().equals(pair.getRight().getId()))
                                        .findFirst();
                                return List.of(leftIdx.orElse(-1), rightIdx.orElse(-1)).toArray(new Long[2]);
                            })
                            .toArray(Long[][]::new);
                })
                .orElse(null);
        val feedback = lastInteraction
                .map(i -> FeedbackDto.builder()
                        .totalSteps(totalInteractionsCount)
                        .stepsWithErrors(interactionsWithErrorsCount)
                        .grade(i.getFeedback().getGrade())
                        .stepsLeft(i.getFeedback().getInteractionsLeft())
                        .build())
                .orElse(null);

        switch (question.getQuestionType()) {
            case ORDER:
                return QuestionDto.builder()
                        .attemptId(question.getExerciseAttempt().getId())
                        .questionId(question.getId())
                        .type(question.getQuestionType().toString())
                        .answers(new QuestionAnswerDto[0])
                        .text(question.getQuestionText())
                        .options(question.getOptions())
                        .responses(responses)
                        .feedback(feedback)
                        .build();
            case MATCHING:
                List<AnswerObjectEntity> answers = question.getAnswerObjects() != null ? question.getAnswerObjects() : new ArrayList<>(0);
                QuestionAnswerDto[] left = IntStream.range(0, answers.size())
                        .filter(i -> !answers.get(i).isRightCol())
                        .mapToObj(i -> new QuestionAnswerDto((long)i, answers.get(i).getHyperText()))
                        .toArray(QuestionAnswerDto[]::new);
                QuestionAnswerDto[] right = IntStream.range(0, answers.size())
                        .filter(i -> answers.get(i).isRightCol())
                        .mapToObj(i -> new QuestionAnswerDto((long)i, answers.get(i).getHyperText()))
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
}
