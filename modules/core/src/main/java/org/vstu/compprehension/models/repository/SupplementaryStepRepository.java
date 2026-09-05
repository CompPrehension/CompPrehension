package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.SupplementaryStepEntity;

import java.util.List;

public interface SupplementaryStepRepository extends CrudRepository<SupplementaryStepEntity, Long> {
    
    SupplementaryStepEntity findBySupplementaryQuestion(QuestionEntity supplementary);

    /** Шаги цепочки вспомогательных вопросов, начатой этим взаимодействием, по порядку. */
    List<SupplementaryStepEntity> findAllByMainQuestionInteractionIdOrderByIdAsc(long interactionId);
}
