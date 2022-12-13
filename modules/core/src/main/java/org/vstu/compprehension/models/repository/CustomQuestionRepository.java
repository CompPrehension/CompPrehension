package org.vstu.compprehension.models.repository;

import org.hibernate.annotations.QueryHints;
import org.springframework.beans.factory.annotation.Autowired;
import org.vstu.compprehension.models.entities.QuestionEntity;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public interface CustomQuestionRepository {
    Optional<QuestionEntity> findByIdEager(Long id);
}
