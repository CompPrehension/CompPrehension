package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.vstu.compprehension.models.entities.SurveyAnswerEntity;

import java.util.Optional;

public interface SurveyAnswerRepository extends CrudRepository<SurveyAnswerEntity, SurveyAnswerEntity.SurveyResultId> {
    @Query("select r from SurveyAnswerEntity r where r.question = ?1 and r.surveyQuestion = ?2 and r.user = ?3")
    Optional<SurveyAnswerEntity> findOne(Long questionId, Long surveyQuestionId, Long userID);
}
