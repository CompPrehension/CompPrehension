package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.SupplementaryStepEntity;

import java.util.List;

public interface SupplementaryStepRepository extends CrudRepository<SupplementaryStepEntity, Long> {

    @Query("select e from SupplementaryStepEntity e " +
            "join fetch e.mainQuestionInteraction " +
            "join fetch e.supplementaryQuestion " +
            "where e.supplementaryQuestion.id = :supplementary")
    SupplementaryStepEntity findBySupplementaryQuestion(@Param("supplementary") long supplementaryQuestionId);

    /** Шаги цепочки вспомогательных вопросов, начатой этим взаимодействием, по порядку. */
    @Query("select e from SupplementaryStepEntity e " +
            "join fetch e.mainQuestionInteraction " +
            "join fetch e.supplementaryQuestion " +
            "where e.mainQuestionInteraction.id = :interactionId " +
            "order by e.id asc")
    List<SupplementaryStepEntity> findAllByMainQuestionInteractionIdOrderByIdAsc(@Param("interactionId") long interactionId);
}
