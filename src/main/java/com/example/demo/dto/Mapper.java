package com.example.demo.dto;

import com.example.demo.dto.question.MatchingQuestionDto;
import com.example.demo.dto.question.QuestionDto;
import com.example.demo.models.entities.AnswerObjectEntity;
import com.example.demo.models.entities.QuestionEntity;
import com.example.demo.models.entities.UserEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Mapper {

    public static UserInfoDto toDto(UserEntity user) {
        return UserInfoDto.builder()
                .id(user.getId())
                .displayName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(Enum::toString).collect(Collectors.toList()))
                .build();
    }

    public static QuestionDto toDto(QuestionEntity question) {
        switch (question.getQuestionType()) {
            case ORDER:
                return QuestionDto.builder()
                        .attemptId(question.getExerciseAttempt().getId())
                        .questionId(question.getId())
                        .type(question.getQuestionType().toString())
                        .answers(new QuestionAnswerDto[0])
                        .text(question.getQuestionText())
                        .options(question.getOptions())
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
                        .build();
            default:
                throw new UnsupportedOperationException("Invalid mapping");
        }
    }
}
