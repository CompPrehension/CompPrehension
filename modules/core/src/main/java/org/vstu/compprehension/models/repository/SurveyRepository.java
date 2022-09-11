package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.vstu.compprehension.models.entities.SurveyEntity;
import org.vstu.compprehension.models.entities.SurveyQuestionEntity;

import java.util.Optional;

public interface SurveyRepository extends CrudRepository<SurveyEntity, String> {
    @Query("select s from SurveyEntity s left join fetch s.questions q where s.surveyId = ?1")
    Optional<SurveyEntity> findOne(String id);

    @Query("select s from SurveyQuestionEntity s where s.id = ?1")
    Optional<SurveyQuestionEntity> findSurveyQuestion(Long questionId);
}
