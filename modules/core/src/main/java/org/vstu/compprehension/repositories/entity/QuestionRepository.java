package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.vstu.compprehension.enums.QuestionType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.QuestionEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {

    @Query("""
            select q from QuestionEntity q
            left join fetch q.metadata m
            left join fetch m.questionData
            where q.id = :questionId
            """)
    Optional<QuestionEntity> findByIdFetchingMetadata(@Param("questionId") long questionId);

    /** Варианты ответа вопроса, в порядке {@code answerId}. */
    @Query("""
            select distinct q from QuestionEntity q
            left join fetch q.answerObjects
            where q.id = :questionId
            """)
    Optional<QuestionEntity> findByIdFetchingAnswerObjects(@Param("questionId") long questionId);

    @Query("select q.domainId from QuestionEntity q where q.id = :questionId")
    Optional<String> findDomainId(@Param("questionId") long questionId);

    @Query("select q.questionType from QuestionEntity q where q.id = :questionId")
    Optional<QuestionType> findQuestionType(@Param("questionId") long questionId);

    /**
     * Id пользователя, которому принадлежит попытка, породившая вопрос.
     */
    @Query("select q.exerciseAttempt.user.id from QuestionEntity q where q.id = :questionId")
    Optional<Long> findOwnerUserId(@Param("questionId") Long questionId);

    /**
     * Вопросы попытки вместе с метаданными.
     */
    @Query("""
            select q from QuestionEntity q
            left join fetch q.metadata
            where q.exerciseAttempt.id = :attemptId
            order by q.id
            """)
    List<QuestionEntity> findAllByAttemptIdFetchingMetadata(@Param("attemptId") long attemptId);

    /** Этап упражнения, для которого задан вопрос; пусто, если вопрос задан не для этапа. */
    @Query("select q.stageIndex from QuestionEntity q where q.id = :questionId")
    Optional<Integer> findStageIndex(@Param("questionId") long questionId);
}
