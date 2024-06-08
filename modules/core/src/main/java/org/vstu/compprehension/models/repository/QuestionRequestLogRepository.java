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

    @Query("SELECT r FROM QuestionRequestLogEntity r WHERE r.domainShortname = :domainShortName AND r.outdated = 0 AND r.foundCount <= :countThreshold AND r.createdDate >= :createdAfter")
    List<QuestionRequestLogEntity> findAllNotProcessed(
            @Param("domainShortName") String domainShortName,
            @Param("createdAfter") LocalDateTime createdAfter,
            @Param("countThreshold") int countThreshold
    );
}














