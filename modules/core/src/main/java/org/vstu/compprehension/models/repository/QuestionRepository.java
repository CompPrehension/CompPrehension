package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.QuestionEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {

    /**
     * Вопрос с метаданными и сериализованным телом из банка заданий.
     * <p>
     * Первый из запросов, которыми {@code QuestionDataRepository} собирает полный
     * {@code QuestionData}. Метаданные могут отсутствовать (вопрос сгенерирован не из
     * банка), поэтому left join; тело банка, наоборот, обязательно, если метаданные есть.
     */
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

    /** Имя домена вопроса — без подъёма самой сущности домена. */
    @Query("select q.domainEntity.name from QuestionEntity q where q.id = :questionId")
    Optional<String> findDomainName(@Param("questionId") long questionId);

    /**
     * Id пользователя, которому принадлежит попытка, породившая вопрос.
     */
    @Query("select q.exerciseAttempt.user.id from QuestionEntity q where q.id = :questionId")
    Optional<Long> findOwnerUserId(@Param("questionId") Long questionId);

    /**
     * Вопросы попытки вместе с метаданными — одним запросом, в порядке выдачи.
     * <p>
     * Порядок задан явно: у {@code ExerciseAttemptEntity.questions} нет {@code @OrderBy},
     * и код полагался на то, в каком порядке строки вернёт БД.
     */
    @Query("""
            select q from QuestionEntity q
            left join fetch q.metadata
            where q.exerciseAttempt.id = :attemptId
            order by q.id
            """)
    List<QuestionEntity> findAllByAttemptIdFetchingMetadata(@Param("attemptId") long attemptId);

    /**
     * Порядковый номер вопроса внутри попытки, считая с единицы.
     * <p>
     * Раньше это считалось как {@code attempt.getQuestions().indexOf(question) + 1},
     * то есть ради одного числа поднимались все вопросы попытки. Порядок тот же:
     * у коллекции нет {@code @OrderBy}, и фактически она приходила в порядке id.
     */
    @Query("""
            select count(q) from QuestionEntity q
            where q.exerciseAttempt.id = :attemptId and q.id <= :questionId
            """)
    long countUpToQuestionInAttempt(@Param("attemptId") long attemptId, @Param("questionId") long questionId);
}
