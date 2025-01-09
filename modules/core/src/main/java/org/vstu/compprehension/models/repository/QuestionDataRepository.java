package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.vstu.compprehension.models.entities.QuestionDataEntity;

import java.util.Optional;

public interface QuestionDataRepository extends CrudRepository<QuestionDataEntity, Integer> {
    @Query("select q from QuestionDataEntity q inner join QuestionMetadataEntity m on q.id = m.questionData.id where m.id = ?1")
    Optional<QuestionDataEntity> findByMetadataId(int questionMetadataId);
}
