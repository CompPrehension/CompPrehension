package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.QuestionEntity;

import java.util.Optional;

public interface CustomQuestionRepository {
    Optional<QuestionEntity> findByIdEager(Long id);
}
