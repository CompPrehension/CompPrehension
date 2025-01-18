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
        final int BATCH = 8 * 1024;
        int lastId = 0;
        int count = 0;
        while (true) {
            List<QuestionMetadataEntity> batchList = qMetaRepo.loadPageWithData(lastId, BATCH);
            System.err.println("New batch loaded");
            if (batchList.isEmpty()) {
                break;
            }
            ArrayList<QuestionMetadataEntity> newMeta = new ArrayList<>();
            for (QuestionMetadataEntity meta : batchList) {
                if (!meta.getDomainShortname().equals("expression_dt")) {
                    continue;
                }
                count++;
                if (count % 5000 == 0) {
                    System.err.printf("Processed %d metadata%n", count);
                }
                System.err.printf("Processing metadata id=%d%n", meta.getId());
                QuestionMetadataEntity obj = MeaningTreeOrderQuestionBuilder.metadataRecalculate(domain, meta);
                obj.setId(meta.getId());
                obj.setQuestionData(meta.getQuestionData());
                obj.setGenerationRequestId(meta.getGenerationRequestId());
                obj.setCreatedAt(meta.getCreatedAt());
                newMeta.add(obj);
                lastId = meta.getId();
            }
            System.err.println("Saving this batch");
            qMetaRepo.saveAll(newMeta);
        }
    }

}
