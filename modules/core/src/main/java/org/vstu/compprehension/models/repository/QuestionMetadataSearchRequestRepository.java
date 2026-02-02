package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.entities.QuestionMetadataSearchRequestEntity;

import java.util.UUID;

@Repository
public interface QuestionMetadataSearchRequestRepository extends CrudRepository<QuestionMetadataSearchRequestEntity, Long> {
    @Query("select r.searchRequest from QuestionMetadataSearchRequestEntity r where r.questionRequestId = :questionRequestId")
    QuestionBankSearchRequest findSearchRequestByQuestionRequestId(@Param("questionRequestId") UUID questionRequestId);
}
