package org.vstu.compprehension.models.repository;

import org.hibernate.annotations.QueryHints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends CrudRepository<QuestionEntity, Long>, CustomQuestionRepository {
    //@Query(value = "select q from QuestionEntity q where q.id = ?1")
    //@EntityGraph(value="with-interactions")
    //public abstract Optional<QuestionEntity> findByIdEager(Long id);
}

