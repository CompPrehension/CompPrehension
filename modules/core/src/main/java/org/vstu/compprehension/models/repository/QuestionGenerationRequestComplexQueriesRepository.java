package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.dto.GenerationRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface QuestionGenerationRequestComplexQueriesRepository {
    List<GenerationRequest> findAllActual(String domainShortname, LocalDateTime createdAfter);
}
