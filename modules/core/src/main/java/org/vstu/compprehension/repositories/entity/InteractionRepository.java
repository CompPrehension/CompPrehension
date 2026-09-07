package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.entities.InteractionEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InteractionRepository extends JpaRepository<InteractionEntity, Long> {

    interface InteractionRow {
        Long getQuestionId();
        Long getInteractionId();
        Integer getOrderNumber();
        InteractionType getInteractionType();
        Integer getInteractionsLeft();
    }

    interface InteractionLawRow {
        Long getInteractionId();
        String getLawName();
    }

    @Query("""
            select i.question.id as questionId, i.id as interactionId,
                   i.orderNumber as orderNumber, i.interactionType as interactionType,
                   f.interactionsLeft as interactionsLeft
            from InteractionEntity i
            left join i.feedback f
            where i.question.id in :questionIds
            order by i.id
            """)
    List<InteractionRow> findRowsByQuestionIdIn(@Param("questionIds") Collection<Long> questionIds);

    @Query("""
            select v.interaction.id as interactionId, v.lawName as lawName
            from ViolationEntity v
            where v.interaction.id in :interactionIds
            order by v.id
            """)
    List<InteractionLawRow> findViolationLawsByInteractionIdIn(@Param("interactionIds") Collection<Long> interactionIds);

    @Query("""
            select cl.interaction.id as interactionId, cl.lawName as lawName
            from CorrectLawEntity cl
            where cl.interaction.id in :interactionIds
            order by cl.id
            """)
    List<InteractionLawRow> findCorrectLawsByInteractionIdIn(@Param("interactionIds") Collection<Long> interactionIds);

    /** Взаимодействия вопроса с оценкой и нарушениями. */
    @Query("""
            select distinct i from InteractionEntity i
            left join fetch i.feedback
            left join fetch i.violations
            where i.question.id = :questionId
            """)
    List<InteractionEntity> findAllByQuestionIdFetchingViolations(@Param("questionId") long questionId);

    /** Взаимодействия вопроса с ответами студента и выбранными вариантами. */
    @Query("""
            select distinct i from InteractionEntity i
            left join fetch i.responses r
            left join fetch r.leftAnswerObject
            left join fetch r.rightAnswerObject
            left join fetch r.createdByInteraction
            where i.question.id = :questionId
            """)
    List<InteractionEntity> findAllByQuestionIdFetchingResponses(@Param("questionId") long questionId);

    /** Взаимодействия вопроса с верно применёнными законами. */
    @Query("""
            select distinct i from InteractionEntity i
            left join fetch i.correctLaw
            where i.question.id = :questionId
            """)
    List<InteractionEntity> findAllByQuestionIdFetchingCorrectLaws(@Param("questionId") long questionId);

    /** Вопрос, которому принадлежит взаимодействие. */
    @Query("select i.question.id from InteractionEntity i where i.id = :interactionId")
    Optional<Long> findQuestionId(@Param("interactionId") long interactionId);
}
