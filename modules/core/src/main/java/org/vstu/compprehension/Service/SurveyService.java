package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.dto.survey.SurveyDto;
import org.vstu.compprehension.dto.survey.SurveyResultDto;
import org.vstu.compprehension.models.entities.SurveyAnswerEntity;
import org.vstu.compprehension.models.repository.SurveyAnswerRepository;
import org.vstu.compprehension.models.repository.SurveyRepository;
import org.vstu.compprehension.models.repository.UserRepository;
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
    private final SurveyRepository surveyRepository;
    private final SurveyAnswerRepository surveyAnswerRepository;
    private final UserRepository userRepository;
    private final QuestionService questionService;

    /** Опрос вместе с его вопросами. */
    @Transactional(readOnly = true)
    public @NotNull SurveyDto getSurvey(@NotNull String surveyId) {
        var survey = surveyRepository.findOne(surveyId)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Couldn't find survey with id %s", surveyId)));
        // Маппинг внутри транзакции: Mapper обходит survey.getQuestions().
        return Mapper.toDto(survey);
    }

    /** Ответы пользователя на опрос в рамках одной попытки. */
    @Transactional(readOnly = true)
    public @NotNull List<SurveyResultDto> getUserAttemptVotes(
            long userId, long attemptId, @NotNull String surveyId) {
        return surveyRepository.findUserAttemptVotes(userId, attemptId, surveyId);
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

        var surveyQuestion = surveyRepository.findSurveyQuestion(result.getSurveyQuestionId())
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        "Invalid survey question %s", result.getSurveyQuestionId())));

        var id = new SurveyAnswerEntity.SurveyResultId(
                result.getSurveyQuestionId(), result.getQuestionId(), userId);
        var answer = surveyAnswerRepository.findById(id).orElseGet(SurveyAnswerEntity::new);
        answer.setQuestion(questionService.getQuestionEntity(result.getQuestionId()));
        answer.setSurveyQuestion(surveyQuestion);
        // Существование пользователя уже доказано проверкой владельца выше,
        // поэтому ссылка без запроса.
        answer.setUser(userRepository.getReferenceById(userId));
        answer.setResult(result.getAnswer());
        surveyAnswerRepository.save(answer);
    }
}
