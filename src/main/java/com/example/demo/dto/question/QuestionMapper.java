package com.example.demo.dto.question;

import com.example.demo.dto.QuestionAnswerDto;
import com.example.demo.models.entities.AnswerObjectEntity;
import com.example.demo.models.entities.ExerciseAttemptEntity;
import com.example.demo.models.entities.QuestionEntity;

import java.util.ArrayList;
import java.util.List;

public class QuestionMapper {
    public static QuestionDto toDto(ExerciseAttemptEntity attempt, QuestionEntity question) {
        switch (question.getQuestionType()) {
            case ORDER:
                return QuestionDto.builder()
                        .attemptId(attempt.getId().toString())
                        .questionId(question.getId().toString())
                        .type(question.getQuestionType().toString())
                        .answers(new QuestionAnswerDto[0])
                        .text(question.getQuestionText())
                        .options(question.getOptions())
                        .build();
            case MATCHING:
                List<AnswerObjectEntity> answers = question.getAnswerObjects() != null ? question.getAnswerObjects() : new ArrayList<>(0);
                QuestionAnswerDto[] left = answers.stream()
                        .filter(a -> !a.isRightCol())
                        .map(a -> new QuestionAnswerDto(a.getId().toString(), a.getHyperText()))
                        .toArray(QuestionAnswerDto[]::new);
                QuestionAnswerDto[] right = answers.stream()
                        .filter(a -> a.isRightCol())
                        .map(a -> new QuestionAnswerDto(a.getId().toString(), a.getHyperText()))
                        .toArray(QuestionAnswerDto[]::new);

                return MatchingQuestionDto.builder()
                        .attemptId(attempt.getId().toString())
                        .questionId(question.getId().toString())
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
