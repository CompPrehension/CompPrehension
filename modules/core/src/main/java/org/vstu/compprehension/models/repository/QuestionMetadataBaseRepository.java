package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.util.Collection;
import java.util.List;

// Базовый пользовательский интерфейс
@NoRepositoryBean
public interface QuestionMetadataBaseRepository <T extends QuestionMetadataEntity> extends CrudRepository<T, Integer> {

//    @Query("select * from #{#entityName} q where q._stage = 3", nativeQuery = true)  // not tested
    @Query("select q from #{#entityName} q where q.stage = 3")  // Note: db field `_stage` mapped by entity to `stage`
    List<QuestionMetadataEntity> findAllReady();

//    @Query("select q from #{#entityName} q where q.stage = 3 and q.concept_bits Long:conceptBitEntries")  // Note: db field `_stage` mapped by entity to `stage`
//    List<QuestionMetadataBaseEntity> findAllWithConcepts(Collection<Long> conceptBitEntries, Pageable pageable);

    @Query("select q from #{#entityName} q where q.stage = 3 AND q.conceptBits IN :values")  // Note: db field `concept_bits` mapped by entity to `conceptBits`
    List<QuestionMetadataEntity> findAllWithConcepts(@Param("values") Collection<Long> conceptBitEntries);

    @Query("select q from #{#entityName} q where q.stage = 3 AND q.conceptBits IN :values AND q.templateId NOT IN :ids")
    List<QuestionMetadataEntity> findAllWithConceptsWithoutTemplates(
            @Param("values") Collection<Long> conceptBitEntries,
            @Param("ids") Collection<Integer> templatesIds
    );

//    @Query("select q from #{#entityName} q where q.stage = 3 AND q.conceptBits IN :concepts AND q.lawBits IN :laws")
    List<QuestionMetadataEntity> findAllWithConceptsLaws(
            @Param("concepts") Collection<Long> conceptBitEntries,
            @Param("laws") Collection<Long> lawBitEntries
    );

//    @Query("select q from #{#entityName} q where q.stage = 3 AND q.conceptBits IN :concepts AND q.lawBits IN :laws AND q.templateId NOT IN :ids")
    List<QuestionMetadataEntity> findAllWithConceptsLawsWithoutTemplates(
            @Param("concepts") Collection<Long> conceptBitEntries,
            @Param("laws") Collection<Long> lawBitEntries,
            @Param("ids") Collection<Integer> templatesIds
    );

    // @Query ....
    List<QuestionMetadataEntity> findAllWithConceptEntriesLawBitsWithoutTemplates(
            @Param("concepts") Collection<Long> traceConceptBitEntries,
            @Param("conceptD") long deniedConceptsBitmask,
            @Param("lawR") long lawsRequiredBitmask,
            @Param("lawD") long lawsDeniedBitmask,
            @Param("ids") Collection<Integer> templatesIds
    );

    // @Query ....
    List<QuestionMetadataEntity> findAllWithConceptLawBitsWithoutTemplates(
            @Param("conceptR") long conceptsRequiredBitmask,
            @Param("conceptD") long conceptsDeniedBitmask,
            @Param("lawR") long lawsRequiredBitmask,
            @Param("lawD") long lawsDeniedBitmask,
            @Param("ids") Collection<Integer> templatesIds
    );

    // @Query ....
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

    // @Query ....
    List<QuestionMetadataEntity> findSampleAroundComplexityWithoutTemplates(
            @Param("complexity") double complexity,
            @Param("conceptA") long conceptsPreferredBitmask,
            @Param("conceptD") long conceptsDeniedBitmask,
            @Param("lawA") long lawsPreferredBitmask,
            @Param("lawD") long lawsDeniedBitmask,
            @Param("ids") Collection<Integer> templatesIds,
            @Param("lim") int limitNumber,
            @Param("random_pool_lim") int randomPoolLimitNumber
    );
}