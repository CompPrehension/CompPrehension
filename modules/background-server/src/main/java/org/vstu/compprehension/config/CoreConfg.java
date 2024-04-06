package org.vstu.compprehension.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.vstu.compprehension.models.businesslogic.storage.AbstractRdfStorage;
import org.vstu.compprehension.models.businesslogic.storage.QuestionMetadataManager;
import org.vstu.compprehension.models.repository.DomainRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

@Configuration
@EnableJpaRepositories(basePackages="org.vstu.compprehension")
@EntityScan(basePackages="org.vstu.compprehension")
public class CoreConfg {

    @Bean
    AbstractRdfStorage getQuestionBank(
            @Autowired DomainRepository domainRepository,
            @Autowired QuestionMetadataRepository metadataRepository) throws Exception {
        var allDomains = domainRepository.findAll();
        return new AbstractRdfStorage(allDomains, metadataRepository, new QuestionMetadataManager(metadataRepository));
    }
}
