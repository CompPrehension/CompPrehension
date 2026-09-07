package org.vstu.compprehension.questionbank;

import org.vstu.compprehension.businesslogic.*;

import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestQuestionMetadata;

import org.apache.commons.collections4.IteratorUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.repositories.entity.*;

import java.util.stream.Collectors;

@Transactional
public class QuestionBankTests extends AbstractIntegrationTest {
    @Autowired
    private QuestionMetadataRepository questionMetadataRepository;
    @Autowired
    private QuestionGenerationRequestRepository questionGenerationRequestRepository;
    @Autowired
    private QuestionBank questionBank;

    @Test
    public void isMatchAndFindTopRatedMetadataEqualityTest() {

        var generationRequests = IteratorUtils.toList(questionGenerationRequestRepository.findAll().iterator())
                .stream()
                .limit(10)
                .toList();
        for (var genReq : generationRequests) {
            var questionSearchRequest = genReq.getQuestionRequest();

            var matched = questionMetadataRepository.findTopRatedMetadata(questionSearchRequest, 1_000_000);
            var matchedIds = matched.stream().map(QuestionMetadataEntity::getId).collect(Collectors.toSet());
            for (var m : matched) {
                Assertions.assertTrue(questionBank.isMatch(TestQuestionMetadata.toData(m), questionSearchRequest), "Matched metadata should match the search request for metadata " + m.getId());
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
                        Assertions.assertTrue(questionBank.isMatch(TestQuestionMetadata.toData(m), questionSearchRequest), "Not matched metadata should not match the search request for metadata " + m.getId());
                    } else {
                        Assertions.assertFalse(questionBank.isMatch(TestQuestionMetadata.toData(m), questionSearchRequest), "Not matched metadata should not match the search request for metadata " + m.getId());
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
