package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.util.List;

public interface QuestionMetadataComplexQueriesRepository {
    int countQuestions(QuestionBankSearchRequest qr);

    List<QuestionMetadataEntity> findSampleAroundComplexityWithoutQIds(
            QuestionBankSearchRequest qr,
            double complexityWindow,
            int limitNumber,
            int randomPoolLimitNumber
    );
}
