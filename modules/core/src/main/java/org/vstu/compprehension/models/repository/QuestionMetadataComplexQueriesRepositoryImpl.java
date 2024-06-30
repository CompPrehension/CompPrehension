package org.vstu.compprehension.models.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.util.List;

public class QuestionMetadataComplexQueriesRepositoryImpl implements QuestionMetadataComplexQueriesRepository {
    private final EntityManager entityManager;
    private final double complexityWindow = 0.1;

    public QuestionMetadataComplexQueriesRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private void ensureRequestValid(QuestionBankSearchRequest qr) {
        if (qr.getStepsMin() > qr.getStepsMax()) {
            throw new IllegalArgumentException("Invalid bank search request: stepsMin > stepsMax");
        }

        if (qr.getStepsMin() == 0 && qr.getStepsMax() == 0) {
            throw new IllegalArgumentException("Invalid bank search request: stepsMin == 0 && stepsMax == 0");
        }
    }

    @Override
    public int countQuestions(QuestionBankSearchRequest qr) {
        ensureRequestValid(qr);
        
        var domainShortname = qr.getDomainShortname();
        var stepsMin = qr.getStepsMin();
        var stepsMax = qr.getStepsMax();
        var deniedConceptBits = qr.getDeniedConceptsBitmask();
        var deniedLawBits = qr.getDeniedLawsBitmask();
        var deniedQuestionNames = qr.getDeniedQuestionNames() == null || qr.getDeniedQuestionNames().isEmpty()
                ? null
                : qr.getDeniedQuestionNames();
        var deniedQuestionTemplateIds = qr.getDeniedQuestionTemplateIds() == null || qr.getDeniedQuestionTemplateIds().isEmpty()
                ? null
                : qr.getDeniedQuestionTemplateIds();
        var deniedQuestionMetaIds = qr.getDeniedQuestionMetaIds() == null || qr.getDeniedQuestionMetaIds().isEmpty()
                ? null
                : qr.getDeniedQuestionMetaIds();
        var targetConceptsBitmask = qr.getTargetConceptsBitmask();
        var targetLawsBitmask = qr.getTargetLawsBitmask();
        var targetTagsBitmask = qr.getTargetTagsBitmask();
        var complexity = qr.getComplexity();

        var query = entityManager.createNativeQuery(
                "select count(*) as number from questions_meta q where " +
                    "q.domain_shortname = :domainShortname " +
                    "AND q.solution_steps >= :stepsMin " +
                    "AND q.solution_steps <= :stepsMax " +
                    "AND q.concept_bits & :deniedConceptBits = 0 " +
                    "AND q.violation_bits & :deniedLawBits = 0 " +
                    "AND (COALESCE(:deniedQuestionNames) IS NULL OR q.name NOT IN (:deniedQuestionNames)) " +
                    "AND (COALESCE(:deniedQuestionTemplateIds) IS NULL OR q.template_id NOT IN (:deniedQuestionTemplateIds)) " +
                    "AND (COALESCE(:deniedQuestionMetaIds) IS NULL OR q.id NOT IN (:deniedQuestionMetaIds)) " +
                    "AND IF(:targetTagsBitmask <> 0, (q.tag_bits & :targetTagsBitmask) = :targetTagsBitmask, 1) " +

                    "AND q.integral_complexity BETWEEN :complexity - :complWindow AND :complexity + :complWindow " +
                    "AND IF(:targetConceptsBitmask <> 0, (q.trace_concept_bits & :targetConceptsBitmask) <> 0, 1) " +
                    "AND IF(:targetLawsBitmask <> 0, (q.violation_bits & :targetLawsBitmask) <> 0, 1) ", Integer.class)
                .setParameter("domainShortname", domainShortname)
                .setParameter("stepsMin", stepsMin)
                .setParameter("stepsMax", stepsMax)
                .setParameter("deniedConceptBits", deniedConceptBits)
                .setParameter("deniedLawBits", deniedLawBits)
                .setParameter("deniedQuestionNames", deniedQuestionNames)
                .setParameter("deniedQuestionTemplateIds", deniedQuestionTemplateIds)
                .setParameter("deniedQuestionMetaIds", deniedQuestionMetaIds)
                .setParameter("targetTagsBitmask", targetTagsBitmask)
                .setParameter("targetConceptsBitmask", targetConceptsBitmask)
                .setParameter("targetLawsBitmask", targetLawsBitmask)
                .setParameter("complWindow", complexityWindow)
                .setParameter("complexity", complexity);
        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public List<Integer> findMostUsedMetadataIds(@Nullable Integer weekUsageThreshold, @Nullable Integer dayUsageThreshold, @Nullable Integer hourUsageThreshold, @Nullable Integer min15UsageThreshold, @Nullable Integer min5UsageThreshold) {
        var query = entityManager.createNativeQuery(
                "SELECT metadata_id FROM (" +
                        "SELECT meta.id as metadata_id, " +
                        //"COUNT(q.metadata_id) OVER (PARTITION BY meta.id) AS used_count_all, " +
                        "COUNT(case when q.created_at > DATE_SUB(NOW(), INTERVAL 1 WEEK) then q.metadata_id end) OVER (PARTITION BY meta.id) AS used_count_week, " +
                        "COUNT(case when q.created_at > DATE_SUB(NOW(), INTERVAL 1 DAY) then q.metadata_id end) OVER (PARTITION BY meta.id) AS used_count_day, " +
                        "COUNT(case when q.created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR) then q.metadata_id end) OVER (PARTITION BY meta.id) AS used_count_hour, " +
                        "COUNT(case when q.created_at > DATE_SUB(NOW(), INTERVAL 15 MINUTE) then q.metadata_id end) OVER (PARTITION BY meta.id) AS used_count_15_minutes, " +
                        "COUNT(case when q.created_at > DATE_SUB(NOW(), INTERVAL 5 MINUTE) then q.metadata_id end) OVER (PARTITION BY meta.id) AS used_count_5_minutes " +
                        "FROM questions_meta meta " +
                        "JOIN question q ON meta.id = q.metadata_id " +
                    ") as source " +
                    "group by metadata_id " +
                    "HAVING MAX(used_count_week) >= :weekUsageThreshold OR MAX(used_count_day) >= :dayUsageThreshold OR MAX(used_count_hour) >= :hourUsageThreshold OR MAX(used_count_15_minutes) >= :min15UsageThreshold OR MAX(used_count_5_minutes) >= :min5UsageThreshold " +
                    "ORDER BY MAX(used_count_5_minutes) DESC, MAX(used_count_15_minutes) DESC, MAX(used_count_hour) DESC", Integer.class)
                .setParameter("weekUsageThreshold", (weekUsageThreshold == null || weekUsageThreshold == 0) ? Integer.MAX_VALUE : weekUsageThreshold)
                .setParameter("dayUsageThreshold", (dayUsageThreshold == null || dayUsageThreshold == 0) ? Integer.MAX_VALUE : dayUsageThreshold)
                .setParameter("hourUsageThreshold", (hourUsageThreshold == null || hourUsageThreshold == 0) ? Integer.MAX_VALUE : hourUsageThreshold)
                .setParameter("min15UsageThreshold", (min15UsageThreshold == null || min15UsageThreshold == 0) ? Integer.MAX_VALUE : min15UsageThreshold)
                .setParameter("min5UsageThreshold", (min5UsageThreshold == null || min5UsageThreshold == 0) ? Integer.MAX_VALUE : min5UsageThreshold);
        //noinspection unchecked
        return (List<Integer>)query.getResultList();        
    }

    public List<QuestionMetadataEntity> findTopRatedMetadata(QuestionBankSearchRequest qr, int limitNumber) {
        ensureRequestValid(qr);

        var domainShortname = qr.getDomainShortname();
        var stepsMin = qr.getStepsMin();
        var stepsMax = qr.getStepsMax();
        var deniedConceptBits = qr.getDeniedConceptsBitmask();
        var deniedLawBits = qr.getDeniedLawsBitmask();
        var deniedQuestionNames = qr.getDeniedQuestionNames() == null || qr.getDeniedQuestionNames().isEmpty()
                ? null
                : qr.getDeniedQuestionNames();
        var deniedQuestionTemplateIds = qr.getDeniedQuestionTemplateIds() == null || qr.getDeniedQuestionTemplateIds().isEmpty()
                ? null
                : qr.getDeniedQuestionTemplateIds();
        var deniedQuestionMetaIds = qr.getDeniedQuestionMetaIds() == null || qr.getDeniedQuestionMetaIds().isEmpty()
                ? null
                : qr.getDeniedQuestionMetaIds();
        var targetConceptsBitmask = qr.getTargetConceptsBitmask();
        var targetLawsBitmask = qr.getTargetLawsBitmask();
        var targetTagsBitmask = qr.getTargetTagsBitmask();
        var complexity = qr.getComplexity();
        var unwantedConceptsBitmask = qr.getUnwantedConceptsBitmask();
        var unwantedLawsBitmask = qr.getUnwantedLawsBitmask();
        var unwantedViolationsBitmask = qr.getUnwantedViolationsBitmask();

        var result = entityManager.createNativeQuery(
                "select * from questions_meta q where " +
                        "q.domain_shortname = :domainShortname " +
                        "AND q.solution_steps >= :stepsMin " +
                        "AND q.solution_steps <= :stepsMax " +
                        "AND q.concept_bits & :deniedConceptBits = 0 " +
                        "AND q.violation_bits & :deniedLawBits = 0 " +
                        "AND (COALESCE(:deniedQuestionNames) IS NULL OR q.name NOT IN (:deniedQuestionNames)) " +
                        "AND (COALESCE(:deniedQuestionTemplateIds) IS NULL OR q.template_id NOT IN (:deniedQuestionTemplateIds)) " +
                        "AND (COALESCE(:deniedQuestionMetaIds) IS NULL OR q.id NOT IN (:deniedQuestionMetaIds)) " +
                        "AND IF(:targetTagsBitmask <> 0, (q.tag_bits & :targetTagsBitmask) = :targetTagsBitmask, 1) " +
                        
                        "AND q.integral_complexity BETWEEN :complexity - :complWindow AND :complexity + :complWindow " +
                        "AND IF(:targetConceptsBitmask <> 0, (q.trace_concept_bits & :targetConceptsBitmask) <> 0, 1) " +
                        "AND IF(:targetLawsBitmask <> 0, (q.violation_bits & :targetLawsBitmask) <> 0, 1) " +
                        "AND (SELECT COUNT(*) FROM question WHERE metadata_id = q.id) = 0 " +
                        //"AND bit_count(q.trace_concept_bits & :targetConceptsBitmask) >= bit_count(:targetConceptsBitmask) DIV 2 " +
                        //"AND bit_count(q.violation_bits & :targetLawsBitmask) >= bit_count(:targetLawsBitmask) DIV 2 " +
                        
                        "order by " + 
                        " (GREATEST(bit_count(q.trace_concept_bits & :unwantedConceptsBitmask), bit_count(q.concept_bits & :unwantedConceptsBitmask)) + bit_count(q.law_bits & :unwantedLawsBitmask) + bit_count(q.violation_bits & :unwantedViolationsBitmask)) DIV 3 ASC, " +
                        " GREATEST(bit_count(q.trace_concept_bits & :targetConceptsBitmask), bit_count(q.concept_bits & :targetConceptsBitmask)) + bit_count(q.violation_bits & :targetLawsBitmask) DESC " +
                        "limit :lim " 
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
                .setParameter("targetTagsBitmask", targetTagsBitmask)
                .setParameter("unwantedConceptsBitmask", unwantedConceptsBitmask)
                .setParameter("unwantedLawsBitmask", unwantedLawsBitmask)
                .setParameter("unwantedViolationsBitmask", unwantedViolationsBitmask)
                .setParameter("complexity", complexity)
                .setParameter("complWindow", complexityWindow)
                .setParameter("lim", limitNumber)
                .getResultList();
        //noinspection unchecked
        return (List<QuestionMetadataEntity>)result;
    }

    public List<QuestionMetadataEntity> findMetadata(QuestionBankSearchRequest qr, int limitNumber) {
        ensureRequestValid(qr);

        var domainShortname = qr.getDomainShortname();
        var stepsMin = qr.getStepsMin();
        var stepsMax = qr.getStepsMax();
        var deniedConceptBits = qr.getDeniedConceptsBitmask();
        var deniedLawBits = qr.getDeniedLawsBitmask();
        var deniedQuestionNames = qr.getDeniedQuestionNames() == null || qr.getDeniedQuestionNames().isEmpty()
                ? null
                : qr.getDeniedQuestionNames();
        var deniedQuestionTemplateIds = qr.getDeniedQuestionTemplateIds() == null || qr.getDeniedQuestionTemplateIds().isEmpty()
                ? null
                : qr.getDeniedQuestionTemplateIds();
        var deniedQuestionMetaIds = qr.getDeniedQuestionMetaIds() == null || qr.getDeniedQuestionMetaIds().isEmpty()
                ? null
                : qr.getDeniedQuestionMetaIds();
        var targetConceptsBitmask = qr.getTargetConceptsBitmask();
        var targetLawsBitmask = qr.getTargetLawsBitmask();
        var targetTagsBitmask = qr.getTargetTagsBitmask();
        var complexity = qr.getComplexity();
        var unwantedConceptsBitmask = qr.getUnwantedConceptsBitmask();
        var unwantedLawsBitmask = qr.getUnwantedLawsBitmask();
        var unwantedViolationsBitmask = qr.getUnwantedViolationsBitmask();

        var result = entityManager.createNativeQuery(
                        "select * from questions_meta q where " +
                                "q.domain_shortname = :domainShortname " +
                                "AND q.solution_steps >= :stepsMin " +
                                "AND q.solution_steps <= :stepsMax " +
                                "AND q.concept_bits & :deniedConceptBits = 0 " +
                                "AND q.violation_bits & :deniedLawBits = 0 " +
                                "AND (COALESCE(:deniedQuestionNames) IS NULL OR q.name NOT IN (:deniedQuestionNames)) " +
                                "AND (COALESCE(:deniedQuestionTemplateIds) IS NULL OR q.template_id NOT IN (:deniedQuestionTemplateIds)) " +
                                "AND (COALESCE(:deniedQuestionMetaIds) IS NULL OR q.id NOT IN (:deniedQuestionMetaIds)) " +
                                "AND IF(:targetTagsBitmask <> 0, (q.tag_bits & :targetTagsBitmask) = :targetTagsBitmask, 1) " +
                                "AND q.integral_complexity <= :complexity + :complWindow " +

                                "order by " +
                                " abs(q.integral_complexity - :complexity) DIV :complWindow ASC, " +
                                " (SELECT COUNT(*) FROM question WHERE metadata_id = q.id) ASC, " +  // less often show "hot" questions
                                " (GREATEST(bit_count(q.trace_concept_bits & :unwantedConceptsBitmask), bit_count(q.concept_bits & :unwantedConceptsBitmask)) + bit_count(q.law_bits & :unwantedLawsBitmask) + bit_count(q.violation_bits & :unwantedViolationsBitmask)) DIV 3 ASC, " +
                                " GREATEST(bit_count(q.trace_concept_bits & :targetConceptsBitmask), bit_count(q.concept_bits & :targetConceptsBitmask)) + bit_count(q.violation_bits & :targetLawsBitmask) DESC " +
                                "limit :lim "
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
                .setParameter("targetTagsBitmask", targetTagsBitmask)
                .setParameter("unwantedConceptsBitmask", unwantedConceptsBitmask)
                .setParameter("unwantedLawsBitmask", unwantedLawsBitmask)
                .setParameter("unwantedViolationsBitmask", unwantedViolationsBitmask)
                .setParameter("complexity", complexity)
                .setParameter("complWindow", complexityWindow)
                .setParameter("lim", limitNumber)
                .getResultList();
        //noinspection unchecked
        return (List<QuestionMetadataEntity>)result;
    }

    @SneakyThrows
    @Override
    public void createGenerationRequest(QuestionBankSearchRequest qr, int questionsToGenerate) {
        var objectMapper = new ObjectMapper();
        var questionRequestJson = objectMapper.writeValueAsString(qr);
        entityManager.createNativeQuery(
                "INSERT INTO question_generation_requests " +
                    "(created_at, updated_at, question_request, questions_to_generate, questions_generated, processing_attempts) " +
                    "VALUES " +
                    "(current_timestamp(), current_timestamp(), CAST(:questionRequest AS JSON), :questionsToGenerate, 0, 0) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "updated_at = current_timestamp(), " +
                    "questions_to_generate = LEAST(questions_to_generate + :questionsToGenerate, 30)")
                .setParameter("questionRequest", questionRequestJson)
                .setParameter("questionsToGenerate", questionsToGenerate)
                .executeUpdate();
    }
}
