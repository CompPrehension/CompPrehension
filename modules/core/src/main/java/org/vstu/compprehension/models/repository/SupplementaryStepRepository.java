package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.entities.SupplementaryStepEntity;

import java.util.List;

public interface SupplementaryStepRepository extends JpaRepository<SupplementaryStepEntity, Long> {

    /**
     * Шаг вместе с идентификатором взаимодействия, начавшего цепочку.
     * <p>
     * Сама сущность нужна ради {@code situationInfo}: это json-колонка со своим типом,
     * и в списке выборки ей не место. А связь на взаимодействие не поднимается — из неё
     * нужен только id, и он лежит в колонке внешнего ключа той же строки.
     */
    interface StepRow {
        SupplementaryStepEntity getStep();
        Long getMainQuestionInteractionId();
    }

    @Query("""
            select s as step, s.mainQuestionInteraction.id as mainQuestionInteractionId
            from SupplementaryStepEntity s
            where s.supplementaryQuestion.id = :supplementary
            """)
    StepRow findRowBySupplementaryQuestion(@Param("supplementary") long supplementaryQuestionId);

    /** Шаги цепочки, начатой этим взаимодействием, от последнего к первому. */
    @Query("""
            select s as step, s.mainQuestionInteraction.id as mainQuestionInteractionId
            from SupplementaryStepEntity s
            where s.mainQuestionInteraction.id = :interactionId
            order by s.id desc
            """)
    List<StepRow> findRowsByMainQuestionInteractionIdOrderByIdDesc(@Param("interactionId") long interactionId);
}
