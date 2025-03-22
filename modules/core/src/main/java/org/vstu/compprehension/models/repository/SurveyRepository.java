package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.vstu.compprehension.dto.survey.SurveyResultDto;
import org.vstu.compprehension.models.entities.SurveyEntity;
import org.vstu.compprehension.models.entities.SurveyQuestionEntity;

import java.util.List;
import java.util.Optional;

public interface SurveyRepository extends CrudRepository<SurveyEntity, String> {
    @Query("select s from SurveyEntity s left join fetch s.questions q where s.surveyId = ?1")
    Optional<SurveyEntity> findOne(String id);

    @Query("select s from SurveyQuestionEntity s where s.id = ?1")
    Optional<SurveyQuestionEntity> findSurveyQuestion(Long questionId);

    @Query("select new org.vstu.compprehension.dto.survey.SurveyResultDto(a.surveyQuestion.id, a.question.id, a.result) " +
            " from SurveyAnswerEntity a " +
            " where a.user.id = ?1 and a.question.exerciseAttempt.id = ?2 and a.surveyQuestion.survey.surveyId = ?3" +
            " order by a.surveyQuestion.id")
    List<SurveyResultDto> findUserAttemptVotes(Long userId, Long attemptId, String surveyId);
}
