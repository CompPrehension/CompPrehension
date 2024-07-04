package org.vstu.compprehension.models.repository;

import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.util.List;

public interface QuestionMetadataComplexQueriesRepository {
    int countQuestions(QuestionBankSearchRequest qr, float complexityWindow);

    List<Integer> findMostUsedMetadataIds(@Nullable Integer weekUsageThreshold, @Nullable Integer dayUsageThreshold, @Nullable Integer hourUsageThreshold, @Nullable Integer min15UsageThreshold, @Nullable Integer min5UsageThreshold);

    List<QuestionMetadataEntity> findTopRatedMetadata(QuestionBankSearchRequest qr, float complexityWindow, int limitNumber);

    List<QuestionMetadataEntity> findMetadata(QuestionBankSearchRequest qr, float complexityWindow, int limitNumber);
}
