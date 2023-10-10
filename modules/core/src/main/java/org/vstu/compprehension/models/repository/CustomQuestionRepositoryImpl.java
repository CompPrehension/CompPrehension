package org.vstu.compprehension.models.repository;

import jakarta.persistence.EntityManager;
import org.vstu.compprehension.models.entities.QuestionEntity;

import java.util.Optional;

public class CustomQuestionRepositoryImpl implements CustomQuestionRepository {
    private final EntityManager entityManager;

    public CustomQuestionRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<QuestionEntity> findByIdEager(Long id) {

        var questions = entityManager.createQuery(
                        "select q from QuestionEntity q " +
                           "left join fetch q.interactions i " +
                           "left join fetch i.feedback f " +
                           "where q.id = :questionId", QuestionEntity.class)
                .setParameter("questionId", id)
                .getResultList();
        /*

         questions = entityManager.createQuery(
                        "select distinct q from QuestionEntity q " +
                           "left join fetch q.interactions i " +
                           "left join fetch i.correctLaw l " +
                           "where q.id = :questionId", QuestionEntity.class)
                .setParameter("questionId", id)
                .setHint(QueryHints.PASS_DISTINCT_THROUGH, false)
                .getResultList();

        questions = entityManager.createQuery(
                        "select distinct q from QuestionEntity q " +
                           "left join fetch q.interactions i " +
                           "left join fetch i.violations v " +
                           "where i.question in :questions", QuestionEntity.class)
                .setParameter("questions", questions)
                .setHint(QueryHints.PASS_DISTINCT_THROUGH, false)
                .getResultList()

         */

        return questions.stream().findFirst();
    }
}
