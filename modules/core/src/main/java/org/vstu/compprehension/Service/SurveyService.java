package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.dto.survey.SurveyDto;
import org.vstu.compprehension.dto.survey.SurveyResultDto;
import org.vstu.compprehension.models.data.SurveyVoteData;
import org.vstu.compprehension.models.repository.data.SurveyDataRepository;
import org.vstu.compprehension.utils.Mapper;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Опросы, показываемые поверх вопросов упражнения.
 * <p>
 * Собран, чтобы контроллер перестал ходить в репозитории напрямую: там, кроме нарушения
 * слоёв, не было и транзакции — сущности покидали сессию, а после отключения
 * open-in-view это перестаёт работать.
 */
@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyDataRepository surveys;
    private final QuestionService questionService;

    /** Опрос вместе с его вопросами. */
    @Transactional(readOnly = true)
    public @NotNull SurveyDto getSurvey(@NotNull String surveyId) {
        return Mapper.toDto(surveys.getById(surveyId));
    }

    /** Ответы пользователя на опрос в рамках одной попытки. */
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

    /**
     * Сохраняет ответ пользователя, перезаписывая предыдущий на тот же вопрос опроса.
     *
     * @throws SecurityException если вопрос принадлежит попытке другого пользователя
     */
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
