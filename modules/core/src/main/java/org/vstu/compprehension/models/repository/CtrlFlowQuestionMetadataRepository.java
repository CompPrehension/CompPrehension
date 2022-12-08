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

}