package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.QuestionRequestLogEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuestionRequestLogRepository extends CrudRepository<QuestionRequestLogEntity, Long> {

    @Query("SELECT r FROM QuestionRequestLogEntity r WHERE r.domainShortname = :domainShortName AND r.outdated = 0 AND r.foundCount <= :countThreshold AND r.createdAt >= :createdAfter")
    List<QuestionRequestLogEntity> findAllNotProcessed(
            @Param("domainShortName") String domainShortName,
            @Param("createdAfter") LocalDateTime createdAfter,
            @Param("countThreshold") int countThreshold
    );

    @Query(value = 
            "SELECT qr.* FROM question_request_log qr " +
            "INNER JOIN ( " +
            "SELECT qr.id AS qr_id, " +
                    "meta.id AS meta_id, " +
                    "ROW_NUMBER() OVER (PARTITION BY meta.id ORDER BY q.created_at DESC) AS numb " +
            "FROM question_request_log qr " +
            "INNER JOIN question q ON qr.id = q.question_request_id " +
            "INNER JOIN questions_meta meta ON q.metadata_id = meta.id " +
            "WHERE meta.id IN (:metadataIds) AND qr.created_at >= :createdAfter AND qr.outdated = 0 " +
            ") AS source ON qr.id = source.qr_id " +
            "WHERE source.numb <= 10 ", 
            nativeQuery = true)
    List<QuestionRequestLogEntity> findAllNotProcessedByMetadataIds(@Param("metadataIds") List<Integer> metadataIds, @Param("createdAfter") LocalDateTime createdAfter);
}














