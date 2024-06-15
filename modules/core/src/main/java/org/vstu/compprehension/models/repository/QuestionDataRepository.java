package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.vstu.compprehension.models.entities.QuestionDataEntity;

public interface QuestionDataRepository extends CrudRepository<QuestionDataEntity, Integer> {
}
