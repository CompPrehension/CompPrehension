package org.vstu.compprehension.models.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.QuestionGenerationRequestEntity;

import java.util.List;

@Repository
public interface QuestionGenerationRequestRepository extends CrudRepository<QuestionGenerationRequestEntity, Integer>, QuestionGenerationRequestComplexQueriesRepository {
    /**
     * Обновить статусы запросов на генерацию.
     */
    @Transactional
    @Query(value = 
            "UPDATE question_generation_requests SET " +
            "status = IF((SELECT COUNT(*) FROM questions_meta WHERE generation_request_id = question_generation_requests.id) >= questions_to_generate, 1, 0), " +
            "processing_attempts = processing_attempts + 1," +
            "updated_at = CURRENT_TIMESTAMP() " + 
            "WHERE id IN :generationRequestIds", nativeQuery = true)
    @Modifying
    void updateGenerationRequests(@Param("generationRequestIds") Integer[] generationRequestIds);

    @Query(value = "SELECT id FROM QuestionGenerationRequestEntity WHERE status = :status AND processingAttempts > :processingAttempts")
    List<Integer> findAllIdsByStatusAndProcessingAttemptsGreaterThan(@Param("status") QuestionGenerationRequestEntity.Status status, @Param("processingAttempts") int processingAttempts);
    
    @Transactional
    @Query(value = 
            "UPDATE question_generation_requests SET " +
            "status = 2," +
            "updated_at = CURRENT_TIMESTAMP() " + 
            "WHERE id IN :generationRequestIds", nativeQuery = true)
    @Modifying
    void setCancelled(@Param("generationRequestIds") List<Integer> generationRequestIds);
}

