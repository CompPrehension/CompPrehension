package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.FeedbackEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends CrudRepository<FeedbackEntity, Long> {
}
