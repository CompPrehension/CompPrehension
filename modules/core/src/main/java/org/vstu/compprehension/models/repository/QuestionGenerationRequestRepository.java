package org.vstu.compprehension.models.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.QuestionGenerationRequestEntity;

@Repository
public interface QuestionGenerationRequestRepository extends CrudRepository<QuestionGenerationRequestEntity, Integer>, QuestionGenerationRequestComplexQueriesRepository {
    @Transactional
    @Query(value = 
            "UPDATE question_generation_requests SET " +
            "is_completed = (SELECT COUNT(*) FROM questions_meta WHERE generation_request_id IN :generationRequestIds) >= (SELECT * FROM (SELECT SUM(questions_to_generate) FROM question_generation_requests WHERE id IN :generationRequestIds) as something), " +
            "processing_attempts = processing_attempts + 1," +
            "updated_at = NOW() " + 
            "WHERE id IN :generationRequestIds", nativeQuery = true)
    @Modifying
    void updateGeneratorRequest(@Param("generationRequestIds") Integer[] generationRequestIds);
}

