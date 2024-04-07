package org.vstu.compprehension.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.vstu.compprehension.jobs.tasksgeneration.TaskGenerationJobConfig;
import org.vstu.compprehension.models.businesslogic.storage.AbstractRdfStorage;
import org.vstu.compprehension.models.businesslogic.storage.QuestionMetadataManager;
import org.vstu.compprehension.models.repository.DomainRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

import java.util.Objects;

@Configuration
@EnableJpaRepositories(basePackages="org.vstu.compprehension")
@EntityScan(basePackages="org.vstu.compprehension")
public class CoreConfg {

    @Bean
    AbstractRdfStorage getQuestionBank(
            @Autowired DomainRepository domainRepository,
            @Autowired QuestionMetadataRepository metadataRepository,
            @Autowired TaskGenerationJobConfig config) throws Exception {
        var allDomains = domainRepository.findAll();
        
        // overrider default options from db
        for (var domain: allDomains) {
            if (Objects.equals(domain.getName(), "ProgrammingLanguageExpressionDomain")) {
                var options = domain.getOptions();
                options.setStorageUploadFilesBaseUrl(config.getExporter().getStorageUploadFilesBaseUrl().toString());
                options.setStorageDownloadFilesBaseUrl(config.getExporter().getStorageUploadFilesBaseUrl().toString());
                options.setStorageDummyDirsForNewFile(config.getExporter().getStorageDummyDirsForNewFile());
            }
        }
        
        return new AbstractRdfStorage(allDomains, metadataRepository, new QuestionMetadataManager(metadataRepository));
    }
}
