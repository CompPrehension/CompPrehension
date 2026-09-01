package org.vstu.compprehension.questionbank;

import org.vstu.compprehension.models.businesslogic.*;

import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;

import org.apache.commons.collections4.IteratorUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.*;
import org.vstu.compprehension.utils.transactions.TransactionScopeFactory;

import java.util.stream.Collectors;

@Transactional
public class QuestionBankTests extends AbstractIntegrationTest {
    @Autowired
    private QuestionMetadataRepository questionMetadataRepository;
    @Autowired
    private QuestionDataRepository questionDataRepository;
    @Autowired
    private QuestionGenerationRequestRepository questionGenerationRequestRepository;
    @Autowired
    private QuestionMetadataSearchRequestRepository questionSearchRequestLogRepository;
    @Autowired
    private TransactionScopeFactory transactionScopeFactory;

    @Test
    public void isMatchAndFindTopRatedMetadataEqualityTest() {
        var questionBank = new QuestionBank(
                questionMetadataRepository,
                questionDataRepository,
                questionGenerationRequestRepository,
                questionSearchRequestLogRepository,
                transactionScopeFactory
        );

        var generationRequests = IteratorUtils.toList(questionGenerationRequestRepository.findAll().iterator())
                .stream()
                .limit(10)
                .toList();
        for (var genReq : generationRequests) {
            var questionSearchRequest = genReq.getQuestionRequest();

            var matched = questionMetadataRepository.findTopRatedMetadata(questionSearchRequest, 1_000_000);
            var matchedIds = matched.stream().map(QuestionMetadataEntity::getId).collect(Collectors.toSet());
            for (var m : matched) {
                Assertions.assertTrue(questionBank.isMatch(m, questionSearchRequest), "Matched metadata should match the search request for metadata " + m.getId());
            }

            int lastLoadedMetadataId = Integer.MIN_VALUE;
            int pageSize = 10_000;
            while (true) {
                var next = questionMetadataRepository.loadPage(lastLoadedMetadataId, questionSearchRequest.getDomainShortname(), pageSize);
                if (next.isEmpty()) {
                    break;
                }
                for (var m : next) {
                    if (matchedIds.contains(m.getId())) {
                        Assertions.assertTrue(questionBank.isMatch(m, questionSearchRequest), "Not matched metadata should not match the search request for metadata " + m.getId());
                    } else {
                        Assertions.assertFalse(questionBank.isMatch(m, questionSearchRequest), "Not matched metadata should not match the search request for metadata " + m.getId());
                    }
                }
                lastLoadedMetadataId = next.getLast().getId();

                if (next.size() < pageSize) {
                    break;
                }
            }
        }
    }
}
