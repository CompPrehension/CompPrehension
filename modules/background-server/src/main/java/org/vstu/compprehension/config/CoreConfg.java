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
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

import java.util.Objects;

@Configuration
@EnableJpaRepositories(basePackages="org.vstu.compprehension")
@EntityScan(basePackages="org.vstu.compprehension")
public class CoreConfg {

    @Bean
    QuestionBank getQuestionBank(
            @Autowired DomainRepository domainRepository,
            @Autowired QuestionDataRepository questionDataRepository,
            @Autowired QuestionMetadataRepository metadataRepository,
            @Autowired TaskGenerationJobConfig tasks) throws Exception {
        var allDomains = domainRepository.findAll();
        
        // overrider default options from db
        for (var domain: allDomains) {
            if (Objects.equals(domain.getName(), "ProgrammingLanguageExpressionDomain")) {
                var options = domain.getOptions();
                var config = tasks.getTasks().stream().filter(t -> t.getDomainShortName().equals("expression")).findFirst().orElseThrow();
                options.setStorageUploadFilesBaseUrl(config.getExporter().getStorageUploadFilesBaseUrl().toString());
                options.setStorageDownloadFilesBaseUrl(config.getExporter().getStorageUploadFilesBaseUrl().toString());
                options.setStorageDummyDirsForNewFile(config.getExporter().getStorageDummyDirsForNewFile());
            }
        }
        
        return new QuestionBank(metadataRepository, questionDataRepository, new QuestionMetadataManager(metadataRepository));
    }
}
