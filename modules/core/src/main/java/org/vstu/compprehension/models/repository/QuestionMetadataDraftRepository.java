package org.vstu.compprehension.models.repository;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.entities.QuestionMetadataDraftEntity;

import java.util.List;


public interface QuestionMetadataDraftRepository extends CrudRepository<QuestionMetadataDraftEntity, Integer> {

    @NotNull
    @Query("select q from #{#entityName} q where q.name = :questionName")
    List<QuestionMetadataDraftEntity> findByName(@Param("questionName") String questionName);

    /*
    @NotNull
    @Query("select q from #{#entityName} q where q.domainShortname = :domainName AND q.stage = :stage")
    List<QuestionMetadataDraftEntity> findAll(
            @Param("domainName") String domainName,
            @Param("stage") int stage  // 1,2,3 or 4
    );

    @Query(value = "select * from questions_meta q where q.domain_shortname = :domainName AND q._stage = 3 " +
            "AND IF(:conceptA =0,1,q.concept_bits & :conceptA <> 0) AND q.concept_bits & :conceptD = 0 " +
            "AND IF(:lawA =0,1,q.violation_bits & :lawA <> 0) AND q.violation_bits & :lawD = 0 " +
            "AND q.template_id NOT IN :ids " +
            "order by bit_count(q.concept_bits & :conceptA)+bit_count(q.violation_bits & :lawA) DESC, abs((q.integral_complexity - :complexity)*13) * abs(q.solution_steps - :solutionSteps + 0.5) limit :lim",
            //  order by abs((integral_complexity - 0.4)*27) * abs(solution_steps - 20 + 0.5) limit 42
            nativeQuery = true)
    List<QuestionMetadataDraftEntity> findSampleAroundComplexityStepsWithoutTemplates(
            @Param("domainName") String domainName,
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
            "select * from questions_meta q where q.domain_shortname = :domainName AND q._stage = 3 " +
            "AND q.solution_steps >= :stepsMin " +
            "AND q.solution_steps <= :stepsMax " +
            "AND q.solution_structural_complexity <= :stepsMax " +  // for Expr domain only
            "AND q.concept_bits & :conceptD = 0 " +
            "AND q.violation_bits & :lawD = 0 " +
//            "AND (IF(:conceptA =0,1,q.concept_bits & :conceptA <> 0) " +
//            "  OR IF(:lawA =0,1,q.violation_bits & :lawA <> 0)) " +
            "AND q.template_id NOT IN :templateIDs " +
            "AND q.id NOT IN :questionIDs " +
            "order by bit_count(q.concept_bits & :conceptA) + bit_count(q.violation_bits & :lawA) + IF(abs(q.integral_complexity - :complexity) <= :complWindow, +10, -abs(q.integral_complexity - :complexity)) DESC " +
            "limit :randomPoolLim" +
            ") T1 ORDER BY ((T1.concept_bits & :conceptA <> 0) + (T1.violation_bits & :lawA <> 0)) DESC, RAND() limit :lim",
            nativeQuery = true)
    List<QuestionMetadataDraftEntity> findSampleAroundComplexityWithoutTemplates(
            @Param("domainName") String domainName,
            @Param("complexity") double complexity,
            @Param("complWindow") double complexityWindow,
            @Param("stepsMin") int solutionStepsMin,
            @Param("stepsMax") int solutionStepsMax,
            @Param("conceptA") long conceptsPreferredBitmask,
            @Param("conceptD") long conceptsDeniedBitmask,
            @Param("lawA") long lawsPreferredBitmask,
            @Param("lawD") long lawsDeniedBitmask,
            @Param("templateIDs") Collection<Integer> templatesIds,
            @Param("questionIDs") Collection<Integer> questionsIds,
            @Param("lim") int limitNumber,
            @Param("randomPoolLim") int randomPoolLimitNumber
    );
    */
}