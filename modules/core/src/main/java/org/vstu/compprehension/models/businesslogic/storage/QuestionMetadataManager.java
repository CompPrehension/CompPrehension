package org.vstu.compprehension.models.businesslogic.storage;

import org.vstu.compprehension.models.entities.QuestionMetadataBaseEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataBaseRepository;

public class QuestionMetadataManager {

    QuestionMetadataBaseRepository<? extends QuestionMetadataBaseEntity> repository;

    public QuestionMetadataManager(
            QuestionMetadataBaseRepository<? extends QuestionMetadataBaseEntity>questionMetadataRepository
    ) {
        this.repository = questionMetadataRepository;

        // test if it works
//        System.out.println("QuestionMetadataManager: begin listing...");
//        repository.findAll().forEach(qme -> System.out.println(qme.getName()));
//        System.out.println("QuestionMetadataManager: end listing.");
    }
}
