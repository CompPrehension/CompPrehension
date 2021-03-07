package com.example.demo.dto.question;

import com.example.demo.dto.QuestionAnswerDto;
import com.example.demo.models.entities.AnswerObjectEntity;
import com.example.demo.models.entities.ExerciseAttemptEntity;
import com.example.demo.models.entities.QuestionEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class QuestionMapper {
    public static QuestionDto toDto(ExerciseAttemptEntity attempt, QuestionEntity question) {
        switch (question.getQuestionType()) {
            case ORDER:
                return QuestionDto.builder()
                        .attemptId(attempt.getId())
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
                        .attemptId(attempt.getId())
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
