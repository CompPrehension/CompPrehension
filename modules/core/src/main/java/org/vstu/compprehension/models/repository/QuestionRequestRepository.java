package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.vstu.compprehension.models.entities.QuestionRequestEntity;

public interface QuestionRequestRepository extends CrudRepository<QuestionRequestEntity, Long> {
}