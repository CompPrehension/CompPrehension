package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.entities.SupplementaryStepEntity;

import java.util.List;

public interface SupplementaryStepRepository extends JpaRepository<SupplementaryStepEntity, Long> {

    interface StepRow {
        SupplementaryStepEntity getStep();
        Long getMainQuestionInteractionId();
        Long getMainQuestionId();
    }

    @Query("""
            select s as step, i.id as mainQuestionInteractionId, q.id as mainQuestionId
            from SupplementaryStepEntity s
            left join s.mainQuestionInteraction i
            left join i.question q
            where s.supplementaryQuestion.id = :supplementary
            """)
    StepRow findRowBySupplementaryQuestion(@Param("supplementary") long supplementaryQuestionId);

    @Query("""
            select s as step, i.id as mainQuestionInteractionId, q.id as mainQuestionId
            from SupplementaryStepEntity s
            left join s.mainQuestionInteraction i
            left join i.question q
            where i.id = :interactionId
            order by s.id desc
            """)
    List<StepRow> findRowsByMainQuestionInteractionIdOrderByIdDesc(@Param("interactionId") long interactionId);
}
