package org.vstu.compprehension.repositories.entity;


import org.springframework.data.jpa.repository.JpaRepository;
import org.vstu.compprehension.entities.AnswerObjectEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerObjectRepository extends JpaRepository<AnswerObjectEntity, Long> {
}
