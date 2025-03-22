package org.vstu.compprehension.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.vstu.compprehension.jobs.tasksgeneration.TaskGenerationJobConfig;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.businesslogic.storage.QuestionMetadataManager;
import org.vstu.compprehension.models.repository.DomainRepository;
import org.vstu.compprehension.models.repository.QuestionDataRepository;
import org.vstu.compprehension.models.repository.QuestionGenerationRequestRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

@Configuration
@EnableJpaRepositories(basePackages="org.vstu.compprehension")
@EntityScan(basePackages="org.vstu.compprehension")
public class CoreConfg {

    @Bean
    QuestionBank getQuestionBank(
            @Autowired DomainRepository domainRepository,
            @Autowired QuestionDataRepository questionDataRepository,
            @Autowired QuestionMetadataRepository metadataRepository,
            @Autowired TaskGenerationJobConfig tasks,
            @Autowired QuestionGenerationRequestRepository generationRequestRepository) throws Exception {
        return new QuestionBank(metadataRepository, questionDataRepository, new QuestionMetadataManager(metadataRepository), generationRequestRepository);
    }
}
