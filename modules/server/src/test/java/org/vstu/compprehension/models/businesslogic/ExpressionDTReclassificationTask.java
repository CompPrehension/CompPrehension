package org.vstu.compprehension.models.businesslogic;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeOrderQuestionBuilder;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

import java.util.ArrayList;
import java.util.List;


@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class ExpressionDTReclassificationTask {
    @Autowired
    DomainFactory domainFactory;
    @Autowired
    private QuestionMetadataRepository qMetaRepo;
    @Autowired
    private QuestionMetadataRepository qDataRepo;

    private ProgrammingLanguageExpressionDTDomain domain;
    public static final String domainId = "ProgrammingLanguageExpressionDTDomain";

    @BeforeAll
    public void tearUp() {
        domain = (ProgrammingLanguageExpressionDTDomain) domainFactory.getDomain(domainId);
    }

    @Test
    @Transactional
    @Commit
    @Rollback(false)
    public void performReclassification() {
        long count = qMetaRepo.count();
        final int BATCH = 50 * 1024;
        for (long i = 1; i <= count; i += BATCH) {
            System.err.printf("Processing: %d/%d%n", i, count);
            List<QuestionMetadataEntity> batchList = qMetaRepo.loadPageWithData((int) i, BATCH);
            ArrayList<QuestionMetadataEntity> newMeta = new ArrayList<>();
            for (QuestionMetadataEntity meta : batchList) {
                if (!meta.getDomainShortname().equals("expression_dt")) {
                    continue;
                }
                QuestionMetadataEntity obj = MeaningTreeOrderQuestionBuilder.metadataRecalculate(domain, meta);
                obj.setId(meta.getId());
                obj.setQuestionData(meta.getQuestionData());
                obj.setGenerationRequestId(meta.getGenerationRequestId());
                obj.setCreatedAt(meta.getCreatedAt());
                newMeta.add(obj);
            }
            qMetaRepo.saveAll(newMeta);
        }
    }

}
