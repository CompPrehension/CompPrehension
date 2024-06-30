package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.entities.QuestionGenerationRequestEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface QuestionGenerationRequestRepository extends CrudRepository<QuestionGenerationRequestEntity, Integer> {
    @Query("select r from QuestionGenerationRequestEntity r " +
            "where r.domainShortname = :domainShortname AND r.questionsGenerated < r.questionsToGenerate AND r.createdAt >= :createdAfter")
    List<QuestionGenerationRequestEntity> findAllActual(@Param("domainShortname") String domainShortname, @Param("createdAfter") LocalDateTime createdAfter);
}

