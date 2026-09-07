package org.vstu.compprehension.frontend;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.frontend.dto.survey.SurveyDto;
import org.vstu.compprehension.frontend.dto.survey.SurveyResultDto;
import org.vstu.compprehension.data.survey.SurveyVoteData;
import org.vstu.compprehension.repositories.data.SurveyDataRepository;
import org.vstu.compprehension.services.QuestionDataService;
import org.vstu.compprehension.mappers.Mapper;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
class SurveyFrontendServiceImpl implements SurveyFrontendService {

    private final SurveyDataRepository surveys;
    private final QuestionDataService questionService;

    @Transactional(readOnly = true)
    public @NotNull SurveyDto getSurvey(@NotNull String surveyId) {
        return Mapper.toDto(surveys.getById(surveyId));
    }

    @Transactional(readOnly = true)
    public @NotNull List<SurveyResultDto> getUserAttemptVotes(
            long userId, long attemptId, @NotNull String surveyId) {
        // Форма ответа API — забота сервиса, слой доступа к данным отдаёт свои записи.
        return surveys.findUserAttemptVotes(userId, attemptId, surveyId).stream()
                .map(v -> SurveyResultDto.builder()
                        .surveyQuestionId(v.surveyQuestionId())
                        .questionId(v.questionId())
                        .answer(v.answer())
                        .build())
                .toList();
    }

    @Transactional
    public void saveAnswer(long userId, @NotNull SurveyResultDto result) {
        var ownerUserId = questionService.findQuestionOwnerUserId(result.getQuestionId())
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        "Question %s does not exist or has no exercise attempt", result.getQuestionId())));
        if (ownerUserId != userId) {
            throw new SecurityException(String.format(
                    "User %s is not allowed to answer surveys for question %s", userId, result.getQuestionId()));
        }

        surveys.saveVote(userId, new SurveyVoteData(
                result.getSurveyQuestionId(), result.getQuestionId(), result.getAnswer()));
    }
}
