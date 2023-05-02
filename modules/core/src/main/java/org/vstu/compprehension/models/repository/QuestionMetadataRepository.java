package org.vstu.compprehension.models.repository;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionRequestLogEntity;

import java.util.Collection;
import java.util.List;


public interface QuestionMetadataRepository extends CrudRepository<QuestionMetadataEntity, Integer> {

    @NotNull
    @Query("select q from #{#entityName} q where q.name = :questionName")
    List<QuestionMetadataEntity> findByName(@Param("questionName") String questionName);

    @NotNull
    @Query("select distinct(q.origin) from #{#entityName} q where q.domainShortname = :domainName")  // ( AND q.stage = :stage ) ?
    List<String> findAllOrigins(
            @Param("domainName") String domainName
    );


    @Query(value = "select * from questions_meta q " +
            "where " +
            "q.domain_shortname = :#{#qr.domainShortname} and " +
            "1 AND q._stage = 3 " +
            "AND q.is_draft = :#{#qr.isDraft} " +
            "AND q.solution_steps >= :#{#qr.stepsMin} " +
            "AND q.solution_steps <= :#{#qr.stepsMax} " +
            "AND q.concept_bits & :#{#qr.conceptsDeniedBitmask} = 0 " +
            "AND q.violation_bits & :#{#qr.lawsDeniedBitmask} = 0 " +

            // at least one targeted concept and one targeted law must present
            "   AND IF(:#{#qr.traceConceptsTargetedBitmask} =0,1,q.trace_concept_bits & :#{#qr.traceConceptsTargetedBitmask} <> 0) " +
            "   AND IF(:#{#qr.conceptsTargetedBitmask} =0,1,q.concept_bits & :#{#qr.conceptsTargetedBitmask} <> 0) " +
            "   AND IF(:#{#qr.lawsTargetedBitmask} =0,1,q.violation_bits & :#{#qr.lawsTargetedBitmask} <> 0) " +
            "limit :lim", nativeQuery = true)
    Collection<QuestionMetadataEntity>
    findSuitableQuestions(
            @Param("qr") QuestionRequestLogEntity qr,
            // // @Param("qr") QuestionRequest qr  // would also work since common fields are the same
             @Param("lim") int limitNumber
    );


    @Query(value = "select q.* from questions_meta q" +
            " LEFT JOIN questions_meta p ON (q.name = p.name AND q.domain_shortname = p.domain_shortname AND p._stage = 3) " +
            " where " +
            "p.id is NULL AND " + // this draft is not in production table
            "q.domain_shortname = :domainShortname AND q._stage = 4 " + // 4 = STAGE_EXPORTED
            "AND q.is_draft = 1 " +
            "", nativeQuery = true)
    List<QuestionMetadataEntity> findNotYetExportedQuestions(
            @Param("domainShortname") String domainShortname
    );

}
