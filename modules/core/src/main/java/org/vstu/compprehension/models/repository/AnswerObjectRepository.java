package org.vstu.compprehension.models.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerObjectRepository extends JpaRepository<AnswerObjectEntity, Long> {
}
