package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.vstu.compprehension.models.entities.SurveyEntity;
import org.vstu.compprehension.models.entities.SurveyQuestionEntity;

import java.util.List;
import java.util.Optional;

public interface SurveyRepository extends JpaRepository<SurveyEntity, String> {
    @Query("select s from SurveyEntity s left join fetch s.questions q where s.surveyId = ?1")
    Optional<SurveyEntity> findOne(String id);

    @Query("select s from SurveyQuestionEntity s where s.id = ?1")
    Optional<SurveyQuestionEntity> findSurveyQuestion(Long questionId);

    /**
     * Голос пользователя в опросе.
     * <p>
     * Своя проекция, а не web-контракт: {@code SurveyResultDto} — это ещё и тело входящего
     * запроса, и его форму диктует фронт. Репозиторий не должен зависеть от неё.
     */
    interface SurveyVoteView {
        Long getSurveyQuestionId();
        Long getQuestionId();
        String getAnswer();
    }

    @Query("select a.surveyQuestion.id as surveyQuestionId, a.question.id as questionId," +
            " a.result as answer" +
            " from SurveyAnswerEntity a " +
            " where a.user.id = ?1 and a.question.exerciseAttempt.id = ?2 and a.surveyQuestion.survey.surveyId = ?3" +
            " order by a.surveyQuestion.id")
    List<SurveyVoteView> findUserAttemptVotes(Long userId, Long attemptId, String surveyId);
}
