package org.vstu.compprehension.models.repository;


import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerObjectRepository extends CrudRepository<AnswerObjectEntity, Long> {
}
