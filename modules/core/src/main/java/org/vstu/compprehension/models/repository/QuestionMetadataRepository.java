package org.vstu.compprehension.models.repository;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionRequestLogEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// Основной интерфейс для поиска вопросов по их метаданным
@Primary
@Repository
public interface QuestionMetadataRepository extends CrudRepository<QuestionMetadataEntity, Integer> {

    @NotNull
    @Override
    Iterable<QuestionMetadataEntity> findAll();

    @NotNull
    @Query("select q from #{#entityName} q where q.name = :questionName")
    List<QuestionMetadataEntity> findByName(@Param("questionName") String questionName);

    @Query(value = "select " +
            "count(*) as 'count', " +
            "min(q.integral_complexity) as min, " +
            "avg(q.integral_complexity) as mean, " +
            "max(q.integral_complexity) as max " +
            "from questions_meta q where q.domain_shortname = :DOMAIN_NAME AND q._stage = 3 " +
            "AND q.is_draft = 0",
            nativeQuery = true)
    Map<String, Object> getStatOnComplexityField(
            @Param("DOMAIN_NAME") String domainShortName
    );

    @Query(value = "SELECT * FROM (" +
            "select * from questions_meta q where " +
            "q.domain_shortname = :#{#qr.domainShortname} AND q._stage = 3 " +
            "AND q.is_draft = :#{#qr.isDraft} " +
            "AND q.solution_steps >= :#{#qr.stepsMin} " +
            "AND q.solution_steps <= :#{#qr.stepsMax} " +
            "AND q.concept_bits & :#{#qr.conceptsDeniedBitmask} = 0 " +
            "AND q.violation_bits & :#{#qr.lawsDeniedBitmask} = 0 " +
            "AND q.template_id NOT IN :#{#qr.deniedQuestionTemplateIds} " +  // note: must be non-empty
            "AND q.id NOT IN :#{#qr.deniedQuestionMetaIds} " + // note: must be non-empty
            "order by bit_count(q.trace_concept_bits & :#{#qr.traceConceptsTargetedBitmask})" +
            " + bit_count(q.concept_bits & :#{#qr.conceptsTargetedBitmask})" +
            " + bit_count(q.violation_bits & :#{#qr.lawsTargetedBitmask})" +
            " DESC, " +
            // // " IF(abs(q.integral_complexity - :#{#qr.complexity}) <= :complWindow, 0, 1)" +
            " abs(q.integral_complexity - :#{#qr.complexity}) DIV :complWindow " +
            " ASC, " +
            " q.used_count ASC " +  // less often show "hot" questions
            " limit :randomPoolLim" +
            ") T1 ORDER BY ((T1.trace_concept_bits & :#{#qr.traceConceptsTargetedBitmask} <> 0) + (T1.concept_bits & :#{#qr.conceptsTargetedBitmask} <> 0) + (T1.violation_bits & :#{#qr.lawsTargetedBitmask} <> 0)) DESC, RAND() limit :lim",
            nativeQuery = true)
    List<QuestionMetadataEntity> findSampleAroundComplexityWithoutQIds(
            @Param("qr") QuestionRequest qr,
            @Param("complWindow") double complexityWindow,
            @Param("lim") int limitNumber,
            @Param("randomPoolLim") int randomPoolLimitNumber
    );

    @Query(value = "select count(*) as number from questions_meta q where " +
            "q.domain_shortname = :#{#qr.domainShortname} AND q._stage = 3 " +
            "AND q.is_draft = :#{#qr.isDraft} " +
            "AND q.solution_steps >= :#{#qr.stepsMin} " +
            "AND q.solution_steps <= :#{#qr.stepsMax} " +
            "AND q.concept_bits & :#{#qr.conceptsDeniedBitmask} = 0 " +
            "AND q.violation_bits & :#{#qr.lawsDeniedBitmask} = 0 " +

            // at least one targeted concept and one targeted law must present
            "   AND IF(:#{#qr.traceConceptsTargetedBitmask} =0,1,q.trace_concept_bits & :#{#qr.traceConceptsTargetedBitmask} <> 0) " +
            "   AND IF(:#{#qr.conceptsTargetedBitmask} =0,1,q.concept_bits & :#{#qr.conceptsTargetedBitmask} <> 0) " +
            "   AND IF(:#{#qr.lawsTargetedBitmask} =0,1,q.violation_bits & :#{#qr.lawsTargetedBitmask} <> 0) " +

            "AND q.template_id NOT IN :#{#qr.deniedQuestionTemplateIds} " +  // note: must be non-empty
            "AND q.id NOT IN :#{#qr.deniedQuestionMetaIds}" + // note: must be non-empty
            "", nativeQuery = true)
    Map<String, Object> countQuestions(@Param("qr") QuestionRequest qr);


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
