package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.SupplementaryStepEntity;

public interface SupplementaryStepRepository extends CrudRepository<SupplementaryStepEntity, Long> {
    
    SupplementaryStepEntity findBySupplementaryQuestion(QuestionEntity supplementary);
}
