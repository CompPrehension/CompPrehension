package org.vstu.compprehension.models.repository;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CtrlFlowQuestionMetadataRepository extends QuestionMetadataBaseRepository<QuestionMetadataEntity> {

    String DOMAIN_NAME = "'ctrl_flow'";
    /* Note: we currently use `violation_bits` column for matching "laws" */

    @NotNull
    @Override
    @Query("select q from #{#entityName} q where q.domainShortname = "+DOMAIN_NAME+" AND q.stage = 3")
    List<QuestionMetadataEntity> findAll();

    @Override
    @Query(value = "select " +
            "count(*) as 'count', " +
            "min(q.integral_complexity) as min, " +
            "avg(q.integral_complexity) as mean, " +
            "max(q.integral_complexity) as max " +
            "from questions_meta q where q.domain_shortname = "+DOMAIN_NAME+" AND q._stage = 3",
            nativeQuery = true)
    Map<String, Object> getStatOnComplexityField();

    @Override
    @Query("select q from #{#entityName} q where q.domainShortname = "+DOMAIN_NAME+" AND q.stage = 3 AND q.conceptBits IN :values")  // Note: db field `concept_bits` mapped by entity to `conceptBits`
    List<QuestionMetadataEntity> findAllWithConcepts(@Param("values") Collection<Long> conceptBitEntries);

    @Override
    @Query("select q from #{#entityName} q where q.domainShortname = "+DOMAIN_NAME+" AND q.stage = 3 AND q.conceptBits IN :values AND q.templateId NOT IN :ids")
    List<QuestionMetadataEntity> findAllWithConceptsWithoutTemplates(
            @Param("values") Collection<Long> conceptBitEntries,
            @Param("ids") Collection<Integer> templatesIds
    );

    @Query("select q from #{#entityName} q where q.domainShortname = "+DOMAIN_NAME+" AND q.stage = 3 AND q.conceptBits IN :concepts AND q.lawBits IN :laws")
    List<QuestionMetadataEntity> findAllWithConceptsLaws(
            @Param("concepts") Collection<Long> conceptBitEntries,
            @Param("laws") Collection<Long> lawBitEntries
    );

    @Query("select q from #{#entityName} q where q.domainShortname = "+DOMAIN_NAME+" AND q.stage = 3 AND q.conceptBits IN :concepts AND q.lawBits IN :laws AND q.templateId NOT IN :ids")
    List<QuestionMetadataEntity> findAllWithConceptsLawsWithoutTemplates(
            @Param("concepts") Collection<Long> conceptBitEntries,
            @Param("laws") Collection<Long> lawBitEntries,
            @Param("ids") Collection<Integer> templatesIds
    );

    @Query(value = "select * from questions_meta q where q.domain_shortname = "+DOMAIN_NAME+" AND q._stage = 3 " +
            "AND q.trace_concept_bits IN :concepts " +
            "AND q.concept_bits & :conceptD = 0 " +
            "AND q.violation_bits & :lawR = :lawR AND q.violation_bits & :lawD = 0 " +
            "AND q.template_id NOT IN :ids",
            nativeQuery = true)
    List<QuestionMetadataEntity> findAllWithConceptEntriesLawBitsWithoutTemplates(
            @Param("concepts") Collection<Long> traceConceptBitEntries,
            @Param("conceptD") long deniedConceptsBitmask,
            @Param("lawR") long lawsRequiredBitmask,
            @Param("lawD") long lawsDeniedBitmask,
            @Param("ids") Collection<Integer> templatesIds
    );

    @Query(value = "select * from questions_meta q where q.domain_shortname = "+DOMAIN_NAME+" AND q._stage = 3 " +
            "AND q.trace_concept_bits & :conceptR = :conceptR AND q.concept_bits & :conceptD = 0 " +
            "AND q.violation_bits & :lawR = :lawR AND q.violation_bits & :lawD = 0 " +
            "AND q.template_id NOT IN :ids",
            nativeQuery = true)
    List<QuestionMetadataEntity> findAllWithConceptLawBitsWithoutTemplates(
            @Param("conceptR") long conceptsRequiredBitmask,
            @Param("conceptD") long conceptsDeniedBitmask,
            @Param("lawR") long lawsRequiredBitmask,
            @Param("lawD") long lawsDeniedBitmask,
            @Param("ids") Collection<Integer> templatesIds
    );

    @Query(value = "select * from questions_meta q where q.domain_shortname = "+DOMAIN_NAME+" AND q._stage = 3 " +
            "AND IF(:conceptA =0,1,q.trace_concept_bits & :conceptA <> 0) AND q.concept_bits & :conceptD = 0 " +
            "AND IF(:lawA =0,1,q.violation_bits & :lawA <> 0) AND q.violation_bits & :lawD = 0 " +
            "AND q.template_id NOT IN :ids " +
            "order by bit_count(q.trace_concept_bits & :conceptA) DESC, abs((q.integral_complexity - :complexity)*27) * abs(q.solution_steps - :solutionSteps + 0.5) limit :lim",
            //  order by abs((integral_complexity - 0.4)*27) * abs(solution_steps - 20 + 0.5) limit 42
            nativeQuery = true)
    List<QuestionMetadataEntity> findSampleAroundComplexityStepsWithoutTemplates(
            @Param("complexity") double complexity,
            @Param("solutionSteps") int solutionSteps,
            @Param("conceptA") long conceptsPreferredBitmask,
            @Param("conceptD") long conceptsDeniedBitmask,
            @Param("lawA") long lawsPreferredBitmask,
            @Param("lawD") long lawsDeniedBitmask,
            @Param("ids") Collection<Integer> templatesIds,
            @Param("lim") int limitNumber
    );

    @Query(value = "SELECT * FROM (" +
            "select * from questions_meta q where q.domain_shortname = "+DOMAIN_NAME+" AND q._stage = 3 " +
            "AND q.solution_steps >= :stepsMin " +
            "AND q.solution_steps <= :stepsMax " +
            "AND q.concept_bits & :conceptD = 0 " +
            "AND q.violation_bits & :lawD = 0 " +
//            "AND (IF(:conceptA =0,1,q.trace_concept_bits & :conceptA <> 0) " +
//            "  OR IF(:lawA =0,1,q.violation_bits & :lawA <> 0)) " +
            "AND q.template_id NOT IN :ids " +
            "order by bit_count(q.trace_concept_bits & :conceptA) + bit_count(q.violation_bits & :lawA) + IF(abs(q.integral_complexity - :complexity) <= :complWindow, +10, -abs(q.integral_complexity - :complexity)) DESC " +
            "limit :randomPoolLim" +
            ") T1 ORDER BY ((T1.concept_bits & :conceptA <> 0) + (T1.violation_bits & :lawA <> 0)) DESC, RAND() limit :lim",
            nativeQuery = true)
    List<QuestionMetadataEntity> findSampleAroundComplexityWithoutTemplates(
            @Param("complexity") double complexity,
            @Param("complWindow") double complexityWindow,
            @Param("stepsMin") int solutionStepsMin,
            @Param("stepsMax") int solutionStepsMax,
            @Param("conceptA") long conceptsPreferredBitmask,
            @Param("conceptD") long conceptsDeniedBitmask,
            @Param("lawA") long lawsPreferredBitmask,
            @Param("lawD") long lawsDeniedBitmask,
            @Param("ids") Collection<Integer> templatesIds,
            @Param("lim") int limitNumber,
            @Param("randomPoolLim") int randomPoolLimitNumber
    );
}