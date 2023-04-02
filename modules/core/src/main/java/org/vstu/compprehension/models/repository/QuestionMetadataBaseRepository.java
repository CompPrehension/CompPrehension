package org.vstu.compprehension.models.repository;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.util.List;
import java.util.Map;

// Базовый интерфейс для поиска вопросов по их метаданным
@Primary
@Repository
public interface QuestionMetadataBaseRepository extends CrudRepository<QuestionMetadataEntity, Integer> {

    @NotNull
    @Override
    Iterable<QuestionMetadataEntity> findAll();

    @Query(value = "select " +
            "count(*) as 'count', " +
            "min(q.integral_complexity) as min, " +
            "avg(q.integral_complexity) as mean, " +
            "max(q.integral_complexity) as max " +
            "from questions_meta q where q.domain_shortname = :DOMAIN_NAME AND q._stage = 3",
            nativeQuery = true)
    Map<String, Object> getStatOnComplexityField(
            @Param("DOMAIN_NAME") String domainShortName
    );


//    // @Query ....
//    List<QuestionMetadataEntity> findAllWithConceptLawBitsWithoutTemplates(
//            @Param("conceptR") long conceptsRequiredBitmask,
//            @Param("conceptD") long conceptsDeniedBitmask,
//            @Param("lawR") long lawsRequiredBitmask,
//            @Param("lawD") long lawsDeniedBitmask,
//            @Param("ids") Collection<Integer> templatesIds
//    );
//
//    // @Query ....
//    List<QuestionMetadataEntity> findSampleAroundComplexityStepsWithoutTemplates(
//            @Param("complexity") double complexity,
//            @Param("solutionSteps") int solutionSteps,
//            @Param("conceptA") long conceptsPreferredBitmask,
//            @Param("conceptD") long conceptsDeniedBitmask,
//            @Param("lawA") long lawsPreferredBitmask,
//            @Param("lawD") long lawsDeniedBitmask,
//            @Param("ids") Collection<Integer> templatesIds,
//            @Param("lim") int limitNumber
//    );
//
//    // @Query ....
//    List<QuestionMetadataEntity> findSampleAroundComplexityWithoutTemplates(
//            @Param("complexity") double complexity,
//            @Param("complWindow") double complexityWindow,
//            @Param("stepsMin") int solutionStepsMin,
//            @Param("stepsMax") int solutionStepsMax,
//            @Param("conceptA") long conceptsPreferredBitmask,
//            @Param("conceptD") long conceptsDeniedBitmask,
//            @Param("lawA") long lawsPreferredBitmask,
//            @Param("lawD") long lawsDeniedBitmask,
//            @Param("templateIDs") Collection<Integer> templatesIds,
//            @Param("questionIDs") Collection<Integer> questionsIds,
//            @Param("lim") int limitNumber,
//            @Param("random_pool_lim") int randomPoolLimitNumber
//    );


    @Query(value = "SELECT * FROM (" +
            "select * from questions_meta q where " +
            "q.domain_shortname = :#{#qr.domainShortname} AND q._stage = 3 " +
            "AND q.solution_steps >= :#{#qr.stepsMin} " +
            "AND q.solution_steps <= :#{#qr.stepsMax} " +
            "AND q.concept_bits & :#{#qr.conceptsDeniedBitmask} = 0 " +
            "AND q.violation_bits & :#{#qr.lawsDeniedBitmask} = 0 " +
            "AND q.template_id NOT IN :#{#qr.deniedQuestionTemplateIds} " +  // note: must be non-empty
            "AND q.id NOT IN :#{#qr.deniedQuestionMetaIds} " + // note: must be non-empty
            "order by bit_count(q.trace_concept_bits & :#{#qr.traceConceptsTargetedBitmask})" +
            " + bit_count(q.concept_bits & :#{#qr.conceptsTargetedBitmask})" +
            " + bit_count(q.violation_bits & :#{#qr.lawsTargetedBitmask})" +
            " + IF(abs(q.integral_complexity - :#{#qr.complexity}) <= :complWindow, +10, -abs(q.integral_complexity - :#{#qr.complexity}))" +
            " - (2 * q.used_count)" +  // less often show "hot" questions
            " DESC limit :randomPoolLim" +
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

}