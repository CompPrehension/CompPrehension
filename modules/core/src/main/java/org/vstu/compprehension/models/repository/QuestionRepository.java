package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.QuestionEntity;

@Repository
public interface QuestionRepository extends CrudRepository<QuestionEntity, Long>, CustomQuestionRepository {
    //@Query(value = "select q from QuestionEntity q where q.id = ?1")
    //@EntityGraph(value="with-interactions")
    //public abstract Optional<QuestionEntity> findByIdEager(Long id);
}

