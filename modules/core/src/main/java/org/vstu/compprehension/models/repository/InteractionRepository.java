package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.EnumData.InteractionType;
import org.vstu.compprehension.models.entities.InteractionEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface InteractionRepository extends CrudRepository<InteractionEntity, Long> {

    /** Взаимодействие без обхода ленивых связей: feedback подтянут join-ом. */
    record InteractionRow(
            Long questionId,
            Long interactionId,
            Integer orderNumber,
            InteractionType interactionType,
            Integer interactionsLeft) {
    }

    /** Имя закона, привязанное к взаимодействию: и для нарушений, и для верно применённых. */
    record InteractionLawRow(Long interactionId, String lawName) {
    }

    /**
     * Взаимодействия перечисленных вопросов, в хронологическом порядке.
     * <p>
     * left join на feedback вместо обращения к связи: она объявлена с {@code @NotFound},
     * то есть грузится жадно отдельным запросом на каждое взаимодействие.
     */
    @Query("""
            select new org.vstu.compprehension.models.repository.InteractionRepository$InteractionRow(
                i.question.id, i.id, i.orderNumber, i.interactionType, f.interactionsLeft)
            from InteractionEntity i
            left join i.feedback f
            where i.question.id in :questionIds
            order by i.id
            """)
    List<InteractionRow> findRowsByQuestionIdIn(@Param("questionIds") Collection<Long> questionIds);

    @Query("""
            select new org.vstu.compprehension.models.repository.InteractionRepository$InteractionLawRow(
                v.interaction.id, v.lawName)
            from ViolationEntity v
            where v.interaction.id in :interactionIds
            order by v.id
            """)
    List<InteractionLawRow> findViolationLawsByInteractionIdIn(@Param("interactionIds") Collection<Long> interactionIds);

    @Query("""
            select new org.vstu.compprehension.models.repository.InteractionRepository$InteractionLawRow(
                cl.interaction.id, cl.lawName)
            from CorrectLawEntity cl
            where cl.interaction.id in :interactionIds
            order by cl.id
            """)
    List<InteractionLawRow> findCorrectLawsByInteractionIdIn(@Param("interactionIds") Collection<Long> interactionIds);
}
