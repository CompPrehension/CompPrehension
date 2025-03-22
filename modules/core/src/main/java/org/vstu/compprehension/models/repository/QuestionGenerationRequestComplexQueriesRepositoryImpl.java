package org.vstu.compprehension.models.repository;

import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.vstu.compprehension.dto.GenerationRequest;
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
    public List<GenerationRequest> findAllActual(String domainShortname, LocalDateTime createdAfter) {
        var query = entityManager.createNativeQuery(
                        "SELECT " +
                                "JSON_UNQUOTE(JSON_ARRAYAGG(r.id)) AS generationrequestids, " +
                                "JSON_UNQUOTE(MAX(r.question_request)) AS questionrequest, " +
                                "CAST(MAX(r.questions_to_generate) + COUNT(*) - 1 AS SIGNED) AS questionstogenerate," +
                                "CAST(SUM(qcount.count) AS SIGNED) AS questionsgenerated " +
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
                            " r.complexity, " +
                            " r.steps_min, " +
                            " r.steps_max", RawFindAllActualResult.class)
                .setParameter("domainShortname", domainShortname)
                .setParameter("createdAfter", createdAfter);

        //noinspection unchecked
        var rawResult = (List<RawFindAllActualResult>)query.getResultList();

        var result = new ArrayList<GenerationRequest>(rawResult.size());
        var gson = new Gson();
        for (var raw : rawResult) {
            result.add(new GenerationRequest(
                    gson.fromJson(raw.getGenerationRequestIds(), Integer[].class),
                    gson.fromJson(raw.getQuestionRequest(), QuestionBankSearchRequest.class),
                    (int)raw.getQuestionsToGenerate(),
                    (int)raw.getQuestionsGenerated()
            ));
        }

        return result;
    }
    
    @Data
    @AllArgsConstructor
    public static class RawFindAllActualResult {
        String generationRequestIds;
        String questionRequest;
        long questionsToGenerate;
        long questionsGenerated;
    }
}
