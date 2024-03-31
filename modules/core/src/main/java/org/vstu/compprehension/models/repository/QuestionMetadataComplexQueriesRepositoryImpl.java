package org.vstu.compprehension.models.repository;

import jakarta.persistence.EntityManager;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.util.List;

public class QuestionMetadataComplexQueriesRepositoryImpl implements QuestionMetadataComplexQueriesRepository {
    private final EntityManager entityManager;

    public QuestionMetadataComplexQueriesRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    public int countQuestions(QuestionBankSearchRequest qr) {
        var domainShortname = qr.getDomainShortname();
        var stepsMin = qr.getStepsMin();
        var stepsMax = qr.getStepsMax();
        var deniedConceptBits = qr.deniedConceptsBitmask();
        var deniedLawBits = qr.deniedLawsBitmask();
        var deniedQuestionNames = qr.getDeniedQuestionNames() == null || qr.getDeniedQuestionNames().isEmpty()
                ? null
                : qr.getDeniedQuestionNames();
        var deniedQuestionTemplateIds = qr.getDeniedQuestionTemplateIds() == null || qr.getDeniedQuestionTemplateIds().isEmpty()
                ? null
                : qr.getDeniedQuestionTemplateIds();
        var deniedQuestionMetaIds = qr.getDeniedQuestionMetaIds() == null || qr.getDeniedQuestionMetaIds().isEmpty()
                ? null
                : qr.getDeniedQuestionMetaIds();
        var targetConceptsBitmask = qr.targetConceptsBitmask();
        var targetLawsBitmask = qr.targetLawsBitmask();
        var complexity = qr.getComplexity();

        var query = entityManager.createNativeQuery(
                "select count(*) as number from questions_meta q where " +
                    "q.domain_shortname = :domainShortname AND q._stage = 3 " +
                    "AND q.solution_steps >= :stepsMin " +
                    "AND q.solution_steps <= :stepsMax " +
                    "AND q.concept_bits & :deniedConceptBits = 0 " +
                    "AND q.violation_bits & :deniedLawBits = 0 " +

                    "AND (COALESCE(:deniedQuestionNames, '') = '' OR q.name NOT IN (:deniedQuestionNames)) " +
                    "AND (COALESCE(:deniedQuestionTemplateIds, '') = '' OR q.template_id NOT IN (:deniedQuestionTemplateIds)) " +
                    "AND (COALESCE(:deniedQuestionMetaIds, '') = '' OR q.id NOT IN (:deniedQuestionMetaIds)) " +

                    "AND IF(:targetConceptsBitmask <> 0, (q.trace_concept_bits & :targetConceptsBitmask) <> 0, 1) " +
                    "AND IF(:targetLawsBitmask <> 0, (q.violation_bits & :targetLawsBitmask) <> 0, 1) " +
                    "AND IF(:complexity <> 0, q.integral_complexity <= :complexity, 1) ", Integer.class)
                .setParameter("domainShortname", domainShortname)
                .setParameter("stepsMin", stepsMin)
                .setParameter("stepsMax", stepsMax)
                .setParameter("deniedConceptBits", deniedConceptBits)
                .setParameter("deniedLawBits", deniedLawBits)
                .setParameter("deniedQuestionNames", deniedQuestionNames)
                .setParameter("deniedQuestionTemplateIds", deniedQuestionTemplateIds)
                .setParameter("deniedQuestionMetaIds", deniedQuestionMetaIds)
                .setParameter("targetConceptsBitmask", targetConceptsBitmask)
                .setParameter("targetLawsBitmask", targetLawsBitmask)
                .setParameter("complexity", complexity);
        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public List<QuestionMetadataEntity> findSampleAroundComplexityWithoutQIds(QuestionBankSearchRequest qr, double complexityWindow, int limitNumber, int randomPoolLimitNumber) {
        var domainShortname = qr.getDomainShortname();
        var stepsMin = qr.getStepsMin();
        var stepsMax = qr.getStepsMax();
        var deniedConceptBits = qr.deniedConceptsBitmask();
        var deniedLawBits = qr.deniedLawsBitmask();
        var deniedQuestionNames = qr.getDeniedQuestionNames() == null || qr.getDeniedQuestionNames().isEmpty()
                ? null
                : qr.getDeniedQuestionNames();
        var deniedQuestionTemplateIds = qr.getDeniedQuestionTemplateIds() == null || qr.getDeniedQuestionTemplateIds().isEmpty()
                ? null
                : qr.getDeniedQuestionTemplateIds();
        var deniedQuestionMetaIds = qr.getDeniedQuestionMetaIds() == null || qr.getDeniedQuestionMetaIds().isEmpty()
                ? null
                : qr.getDeniedQuestionMetaIds();
        var targetConceptsBitmask = qr.targetConceptsBitmask();
        var targetLawsBitmask = qr.targetLawsBitmask();
        var complexity = qr.getComplexity();

        var result = entityManager.createNativeQuery(
            "SELECT * FROM (" +
            "select * from questions_meta q where " +
            "q.domain_shortname = :domainShortname AND q._stage = 3 " +
            "AND q.solution_steps >= :stepsMin " +
            "AND q.solution_steps <= :stepsMax " +
            "AND q.concept_bits & :deniedConceptBits = 0 " +
            "AND q.violation_bits & :deniedLawBits = 0 " +

            "AND (COALESCE(:deniedQuestionNames, NULL) = '' OR q.name NOT IN (:deniedQuestionNames)) " +
            "AND (COALESCE(:deniedQuestionTemplateIds, NULL) = '' OR q.template_id NOT IN (:deniedQuestionTemplateIds)) " +
            "AND (COALESCE(:deniedQuestionMetaIds, NULL) = '' OR q.id NOT IN (:deniedQuestionMetaIds)) " +

            "order by bit_count(q.trace_concept_bits & :targetConceptsBitmask)" +
            " + bit_count(q.concept_bits & :targetConceptsBitmask)" +
            " + bit_count(q.violation_bits & :targetLawsBitmask)" +
            " DESC, " +
            // // " IF(abs(q.integral_complexity - :#{#qr.complexity}) <= :complWindow, 0, 1)" +
            " abs(q.integral_complexity - :complexity) DIV :complWindow " +
            " ASC, " +
            " q.used_count ASC " +  // less often show "hot" questions
            " limit :randomPoolLim" +
            ") T1 ORDER BY ((T1.trace_concept_bits & :targetConceptsBitmask <> 0) + (T1.concept_bits & :targetConceptsBitmask <> 0) + (T1.violation_bits & :targetLawsBitmask <> 0)) DESC, RAND() limit :lim"
                        , QuestionMetadataEntity.class)
                .setParameter("domainShortname", domainShortname)
                .setParameter("stepsMin", stepsMin)
                .setParameter("stepsMax", stepsMax)
                .setParameter("deniedConceptBits", deniedConceptBits)
                .setParameter("deniedLawBits", deniedLawBits)
                .setParameter("deniedQuestionNames", deniedQuestionNames)
                .setParameter("deniedQuestionTemplateIds", deniedQuestionTemplateIds)
                .setParameter("deniedQuestionMetaIds", deniedQuestionMetaIds)
                .setParameter("targetConceptsBitmask", targetConceptsBitmask)
                .setParameter("targetLawsBitmask", targetLawsBitmask)
                .setParameter("complexity", complexity)
                .getResultList();
        //noinspection unchecked
        return (List<QuestionMetadataEntity>)result;
    }
}
