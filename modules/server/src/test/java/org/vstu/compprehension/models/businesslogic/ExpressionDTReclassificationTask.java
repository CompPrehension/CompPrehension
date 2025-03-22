package org.vstu.compprehension.models.businesslogic;

import jakarta.transaction.Transactional;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    @Autowired
    private SessionFactory sessionFactory;

    private ProgrammingLanguageExpressionDTDomain domain;
    public static final String domainId = "ProgrammingLanguageExpressionDTDomain";

    @BeforeAll
    public void tearUp() {
        domain = (ProgrammingLanguageExpressionDTDomain) domainFactory.getDomain(domainId);
    }

    @Test
    public void performReclassification() {
        final int BATCH_SIZE = 8 * 1024;
        int lastId = 0;
        int batches = 0;

        while (true) {
            List<QuestionMetadataEntity> batchList = qMetaRepo.loadPageWithData(lastId, BATCH_SIZE);
            System.err.println("New batch loaded");

            if (batchList.isEmpty()) {
                break;
            }

            lastId = processBatch(batchList, lastId);
            batches++;
            if (batches % 8 == 0) {
                System.gc();
            }
        }
    }

    @Transactional
    @Rollback(false)
    protected int processBatch(List<QuestionMetadataEntity> batchList, int lastId) {
        List<Integer> toDelete = new ArrayList<>();
        List<QuestionMetadataEntity> newMeta = new ArrayList<>();

        for (QuestionMetadataEntity meta : batchList) {
            if (!meta.getDomainShortname().equals("expression_dt")) {
                continue;
            }

            System.err.printf("Processing metadata id=%d%n", meta.getId());
            QuestionMetadataEntity obj = MeaningTreeOrderQuestionBuilder.metadataRecalculate(domain, meta);

            if (obj == null) {
                toDelete.add(meta.getId());
                lastId = meta.getId();
                System.err.println("This metadata will be deleted");
                continue;
            }

            obj.setId(meta.getId());
            obj.setQuestionData(meta.getQuestionData());
            obj.setGenerationRequestId(meta.getGenerationRequestId());
            obj.setCreatedAt(meta.getCreatedAt());
            newMeta.add(obj);
            lastId = meta.getId();
        }

        System.err.println("Saving this batch");
        try (var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            for (QuestionMetadataEntity meta : newMeta) {
                session.saveOrUpdate(meta);
            }
            transaction.commit();
        }
        if (!toDelete.isEmpty()) {
            System.err.printf("Deleting %d metadata %n", toDelete.size());
            qMetaRepo.deleteAllById(toDelete);
        }
        return lastId;
    }
}
