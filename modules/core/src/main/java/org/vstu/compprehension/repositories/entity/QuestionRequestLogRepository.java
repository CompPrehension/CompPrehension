package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.QuestionRequestLogEntity;

@Repository
public interface QuestionRequestLogRepository extends JpaRepository<QuestionRequestLogEntity, Long> {
}
