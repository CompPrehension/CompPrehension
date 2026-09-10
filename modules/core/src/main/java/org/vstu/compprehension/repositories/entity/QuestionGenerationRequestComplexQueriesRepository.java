package org.vstu.compprehension.repositories.entity;

import org.vstu.compprehension.data.questionbank.GenerationRequestGroupData;
import org.vstu.compprehension.businesslogic.QuestionBankSearchRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface QuestionGenerationRequestComplexQueriesRepository {
    List<GenerationRequestGroupData> findAllActual(String domainShortname, LocalDateTime createdAfter);
    int findNumberOfCurrentlyGeneratingQuestions(String domainShortname, QuestionBankSearchRequest request);
}
