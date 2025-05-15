package org.vstu.compprehension.models.repository;

import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.vstu.compprehension.dto.GenerationRequest;
import org.vstu.compprehension.dto.GenerationRequestGroup;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QuestionGenerationRequestComplexQueriesRepositoryImpl implements QuestionGenerationRequestComplexQueriesRepository {
    private final EntityManager entityManager;

    public QuestionGenerationRequestComplexQueriesRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    public List<GenerationRequestGroup> findAllActual(String domainShortname, LocalDateTime createdAfter) {
        var query = entityManager.createNativeQuery(
                        "SELECT " +
                                "JSON_UNQUOTE(JSON_ARRAYAGG(r.id)) AS generationrequestids, " +
                                "JSON_UNQUOTE(JSON_ARRAYAGG(JSON_OBJECT('id', r.id, 'questionsToGenerate', GREATEST(0, r.questions_to_generate - qcount.count)))) AS generationrequests, " +
                                "JSON_UNQUOTE(MAX(r.question_request)) AS questionrequest, " +
                                "CAST(GREATEST(0, SUM(r.questions_to_generate) - SUM(qcount.count)) AS SIGNED) AS questionstogenerate " +
                            "FROM question_generation_requests r " +
                            "LEFT JOIN (" +
                                "SELECT question_generation_requests.id AS id, COUNT(questions_meta.id) as count " +
                                "FROM question_generation_requests " +
                                "LEFT JOIN questions_meta on question_generation_requests.id = questions_meta.generation_request_id " +
                                "GROUP BY question_generation_requests.id " +
                            ") AS qcount ON r.id = qcount.id " +
                            "WHERE r.status = 0 AND r.domain_shortname = :domainShortname AND r.created_at >= :createdAfter " +
                            "GROUP BY " +
                            " r.denied_concepts_bitmask, " +
                            " r.denied_laws_bitmask, " +
                            " r.denied_skills_bitmask, " +
                            " r.target_concepts_bitmask, " +
                            " r.target_laws_bitmask," +
                            " r.target_skills_bitmask," +
                            " r.target_tags_bitmask, " +
                            " TRUNCATE(CAST(r.complexity AS decimal), 3), " +
                            " r.steps_min, " +
                            " r.steps_max", RawFindAllActualResult.class)
                .setParameter("domainShortname", domainShortname)
                .setParameter("createdAfter", createdAfter);

        //noinspection unchecked
        var rawResult = (List<RawFindAllActualResult>)query.getResultList();

        var result = new ArrayList<GenerationRequestGroup>(rawResult.size());
        var gson = new Gson();
        for (var raw : rawResult) {
            result.add(new GenerationRequestGroup(
                    gson.fromJson(raw.getGenerationRequestIds(), Integer[].class),
                    gson.fromJson(raw.getGenerationRequests(), GenerationRequest[].class),
                    gson.fromJson(raw.getQuestionRequest(), QuestionBankSearchRequest.class),
                    (int)raw.getQuestionsToGenerate()
            ));
        }

        return result;
    }

    @Override
    public int findNumberOfCurrentlyGeneratingQuestions(String domainShortname, QuestionBankSearchRequest request) {
        var query = entityManager.createNativeQuery(
                        "SELECT " +
                                "CAST(GREATEST(0, SUM(r.questions_to_generate) - SUM(qcount.count)) AS SIGNED) " +
                                "FROM question_generation_requests r " +
                                "LEFT JOIN (" +
                                    "SELECT question_generation_requests.id AS id, COUNT(questions_meta.id) as count " +
                                    "FROM question_generation_requests " +
                                    "LEFT JOIN questions_meta on question_generation_requests.id = questions_meta.generation_request_id " +
                                    "GROUP BY question_generation_requests.id " +
                                ") AS qcount ON r.id = qcount.id " +
                                "WHERE r.status = 0 AND r.domain_shortname = :domainShortname AND " +
                                "r.steps_min = :stepsMin AND " +
                                "r.steps_max = :stepsMax AND " +
                                "abs(r.complexity - :complexity) < 0.0001 AND " +
                                "r.denied_concepts_bitmask = :deniedConceptsBitmask AND " +
                                "r.denied_laws_bitmask = :deniedLawsBitmask AND " +
                                "r.denied_skills_bitmask = :deniedSkillsBitmask AND " +
                                "r.target_concepts_bitmask = :targetConceptsBitmask AND " +
                                "r.target_laws_bitmask = :targetLawsBitmask AND " +
                                "r.target_skills_bitmask = :targetSkillsBitmask AND " +
                                "r.target_tags_bitmask = :targetTagsBitmask " +
                                "GROUP BY " +
                                " r.denied_concepts_bitmask, " +
                                " r.denied_laws_bitmask, " +
                                " r.denied_skills_bitmask, " +
                                " r.target_concepts_bitmask, " +
                                " r.target_laws_bitmask," +
                                " r.target_skills_bitmask," +
                                " r.target_tags_bitmask, " +
                                " TRUNCATE(CAST(r.complexity AS decimal), 3)," +
                                " r.steps_min, " +
                                " r.steps_max", Integer.class)
                .setParameter("domainShortname", domainShortname)
                .setParameter("stepsMin", request.getStepsMin())
                .setParameter("stepsMax", request.getStepsMax())
                .setParameter("complexity", request.getComplexity())
                .setParameter("deniedConceptsBitmask", request.getDeniedConceptsBitmask())
                .setParameter("deniedLawsBitmask", request.getDeniedLawsBitmask())
                .setParameter("deniedSkillsBitmask", request.getDeniedSkillsBitmask())
                .setParameter("targetConceptsBitmask", request.getTargetConceptsBitmask())
                .setParameter("targetLawsBitmask", request.getTargetLawsBitmask())
                .setParameter("targetSkillsBitmask", request.getTargetSkillsBitmask())
                .setParameter("targetTagsBitmask", request.getTargetTagsBitmask());
        List<Integer> resultList = query.getResultList();
        return (resultList != null && !resultList.isEmpty() && resultList.get(0) != null) ? resultList.get(0) : 0;
    }

    @Data
    @AllArgsConstructor
    public static class RawFindAllActualResult {
        String generationRequestIds;
        String generationRequests;
        String questionRequest;
        long questionsToGenerate;
    }
}
