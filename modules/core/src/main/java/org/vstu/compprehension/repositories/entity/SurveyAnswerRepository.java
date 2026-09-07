package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.vstu.compprehension.entities.SurveyAnswerEntity;

import java.util.Optional;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswerEntity, SurveyAnswerEntity.SurveyResultId> {
    @Query("select r from SurveyAnswerEntity r where r.question = ?1 and r.surveyQuestion = ?2 and r.user = ?3")
    Optional<SurveyAnswerEntity> findOne(Long questionId, Long surveyQuestionId, Long userID);
}
