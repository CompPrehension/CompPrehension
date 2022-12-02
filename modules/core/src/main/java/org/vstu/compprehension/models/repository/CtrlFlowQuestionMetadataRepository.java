package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.entities.QuestionMetaEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataBaseEntity;

import java.util.Collection;
import java.util.List;

public interface CtrlFlowQuestionMetadataRepository extends QuestionMetadataBaseRepository<QuestionMetaEntity> {

    @Override
    @Query("select q from #{#entityName} q where q.domainShortname = 'ctrl_flow' AND q.stage = 3 AND q.conceptBits IN :values")  // Note: db field `concept_bits` mapped by entity to `conceptBits`
    List<QuestionMetadataBaseEntity> findAllWithConcepts(@Param("values") Collection<Integer> conceptBitEntries);

    @Override
    @Query("select q from #{#entityName} q where q.domainShortname = 'ctrl_flow' AND q.stage = 3 AND q.conceptBits IN :values AND q.templateId NOT IN :ids")
    List<QuestionMetadataBaseEntity> findAllWithConceptsWithoutTemplates(
            @Param("values") Collection<Integer> conceptBitEntries,
            @Param("ids") Collection<Integer> templatesIds
    );

}