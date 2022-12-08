package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.util.Collection;
import java.util.List;

public interface CtrlFlowQuestionMetadataRepository extends QuestionMetadataBaseRepository<QuestionMetadataEntity> {

    @Override
    @Query("select q from #{#entityName} q where q.domainShortname = 'ctrl_flow' AND q.stage = 3 AND q.conceptBits IN :values")  // Note: db field `concept_bits` mapped by entity to `conceptBits`
    List<QuestionMetadataEntity> findAllWithConcepts(@Param("values") Collection<Long> conceptBitEntries);

    @Override
    @Query("select q from #{#entityName} q where q.domainShortname = 'ctrl_flow' AND q.stage = 3 AND q.conceptBits IN :values AND q.templateId NOT IN :ids")
    List<QuestionMetadataEntity> findAllWithConceptsWithoutTemplates(
            @Param("values") Collection<Long> conceptBitEntries,
            @Param("ids") Collection<Integer> templatesIds
    );

    @Query("select q from #{#entityName} q where q.domainShortname = 'ctrl_flow' AND q.stage = 3 AND q.conceptBits IN :concepts AND q.lawBits IN :laws")
    List<QuestionMetadataEntity> findAllWithConceptsLaws(
            @Param("concepts") Collection<Long> conceptBitEntries,
            @Param("laws") Collection<Long> lawBitEntries
    );

    @Query("select q from #{#entityName} q where q.domainShortname = 'ctrl_flow' AND q.stage = 3 AND q.conceptBits IN :concepts AND q.lawBits IN :laws AND q.templateId NOT IN :ids")
    List<QuestionMetadataEntity> findAllWithConceptsLawsWithoutTemplates(
            @Param("concepts") Collection<Long> conceptBitEntries,
            @Param("laws") Collection<Long> lawBitEntries,
            @Param("ids") Collection<Integer> templatesIds
    );

    @Query(value = "select * from questions_meta q where q.domain_shortname = 'ctrl_flow' AND q._stage = 3 " +
            "AND q.concept_bits IN :concepts " +
            "AND q.law_bits & :lawR = :lawR AND q.law_bits & :lawD = 0 " +
            "AND q.template_id NOT IN :ids",
            nativeQuery = true)
    List<QuestionMetadataEntity> findAllWithConceptEntriesLawBitsWithoutTemplates(
            @Param("concepts") Collection<Long> conceptBitEntries,
            @Param("lawR") long lawsRequiredBitmask,
            @Param("lawD") long lawsDeniedBitmask,
            @Param("ids") Collection<Integer> templatesIds
    );

    @Query(value = "select * from questions_meta q where q.domain_shortname = 'ctrl_flow' AND q._stage = 3 " +
            "AND q.concept_bits & :conceptR = :conceptR AND q.concept_bits & :conceptD = 0 " +
            "AND q.law_bits & :lawR = :lawR AND q.law_bits & :lawD = 0 " +
            "AND q.template_id NOT IN :ids",
            nativeQuery = true)
    List<QuestionMetadataEntity> findAllWithConceptLawBitsWithoutTemplates(
            @Param("conceptR") long conceptsRequiredBitmask,
            @Param("conceptD") long conceptsDeniedBitmask,
            @Param("lawR") long lawsRequiredBitmask,
            @Param("lawD") long lawsDeniedBitmask,
            @Param("ids") Collection<Integer> templatesIds
    );
}