package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionRepository extends CrudRepository<QuestionEntity, Long> {
    @Query(value =
              "select q from QuestionEntity q" +
              " where q.id = ?1")
    Optional<QuestionEntity> findByIdEager(Long id);
}

