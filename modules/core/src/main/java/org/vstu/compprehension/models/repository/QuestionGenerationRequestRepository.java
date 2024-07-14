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
     * Обновить статус группы равносильных запросов на генерацию вопросов.
     * При этом каждый вопрос, сгенерированный для любого из этих запросов, учитывается один раз для каждого запроса из группы.
     * @param generationRequestIds список «сливающихся» запросов на генерацию
     */
    @Transactional
    @Query(value = 
            "UPDATE question_generation_requests SET " +
            "status = IF((SELECT COUNT(*) FROM questions_meta WHERE generation_request_id IN :generationRequestIds) >= (SELECT * FROM (SELECT MAX(questions_to_generate) + COUNT(*) - 1 FROM question_generation_requests WHERE id IN :generationRequestIds) as something), 1, 0), " +
            "processing_attempts = processing_attempts + 1," +
            "updated_at = CURRENT_TIMESTAMP() " + 
            "WHERE id IN :generationRequestIds", nativeQuery = true)
    @Modifying
    void updateGeneratorRequest(@Param("generationRequestIds") Integer[] generationRequestIds);
    
    
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

