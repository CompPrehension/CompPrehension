package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.data.question.InteractionReasoningData;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.entities.InteractionEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface InteractionRepository extends JpaRepository<InteractionEntity, Long> {

    interface InteractionRow {
        Long getQuestionId();
        Long getInteractionId();
        Integer getOrderNumber();
        InteractionType getInteractionType();
        Integer getInteractionsLeft();
        boolean getIsCorrect();
        List<InteractionReasoningData> getReasonings();
        Long getClarificationId();
        Integer getChosenReasoning();
    }

    @Query("""
            select i.question.id as questionId, i.id as interactionId,
                   i.orderNumber as orderNumber, i.interactionType as interactionType,
                   f.interactionsLeft as interactionsLeft,
                   i.isCorrect as isCorrect, i.reasonings as reasonings,
                   c.id as clarificationId, c.chosenReasoning as chosenReasoning
            from InteractionEntity i
            left join i.feedback f
            left join InteractionClarificationEntity c on c.interaction = i
            where i.question.id in :questionIds
            order by i.id
            """)
    List<InteractionRow> findRowsByQuestionIdIn(@Param("questionIds") Collection<Long> questionIds);

    /** Взаимодействия вопроса с оценкой и уточняющим вопросом. */
    @Query("""
            select distinct i from InteractionEntity i
            left join fetch i.feedback
            left join fetch i.clarification
            where i.question.id = :questionId
            order by i.id
            """)
    List<InteractionEntity> findAllByQuestionIdFetchingFeedback(@Param("questionId") long questionId);

    /** Взаимодействия вопроса с ответами студента и выбранными вариантами. */
    @Query("""
            select distinct i from InteractionEntity i
            left join fetch i.responses r
            left join fetch r.leftAnswerObject
            left join fetch r.rightAnswerObject
            left join fetch r.createdByInteraction
            where i.question.id = :questionId
            order by i.id, r.id
            """)
    List<InteractionEntity> findAllByQuestionIdFetchingResponses(@Param("questionId") long questionId);
}
