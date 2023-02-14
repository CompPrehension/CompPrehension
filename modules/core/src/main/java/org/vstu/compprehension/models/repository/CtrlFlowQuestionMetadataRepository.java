package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.util.Collection;
import java.util.List;

public interface CtrlFlowQuestionMetadataRepository extends QuestionMetadataBaseRepository<QuestionMetadataEntity> {

    String DOMAIN_NAME = "'ctrl_flow'";
    /* Note: we currently use `violation_bits` column for matching "laws" */

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
            "AND IF(:conceptA =0,1,q.trace_concept_bits & :conceptA <> 0) AND q.concept_bits & :conceptD = 0 " +
            "AND IF(:lawA =0,1,q.violation_bits & :lawA <> 0) AND q.violation_bits & :lawD = 0 " +
            "AND q.template_id NOT IN :ids " +
            "order by bit_count(q.trace_concept_bits & :conceptA) + bit_count(q.violation_bits & :lawA) DESC, abs(q.integral_complexity - :complexity)" +
            "limit :randomPoolLim" +
            ") T1 ORDER BY RAND() limit :lim",
            nativeQuery = true)
    List<QuestionMetadataEntity> findSampleAroundComplexityWithoutTemplates(
            @Param("complexity") double complexity,
            @Param("conceptA") long conceptsPreferredBitmask,
            @Param("conceptD") long conceptsDeniedBitmask,
            @Param("lawA") long lawsPreferredBitmask,
            @Param("lawD") long lawsDeniedBitmask,
            @Param("ids") Collection<Integer> templatesIds,
            @Param("lim") int limitNumber,
            @Param("randomPoolLim") int randomPoolLimitNumber
    );
}