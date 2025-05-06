package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.dto.GenerationRequestGroup;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface QuestionGenerationRequestComplexQueriesRepository {
    List<GenerationRequestGroup> findAllActual(String domainShortname, LocalDateTime createdAfter);
    int findNumberOfCurrentlyGeneratingQuestions(String domainShortname, QuestionBankSearchRequest request);
}
