package org.vstu.compprehension.jobs.tasksgeneration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "task-generation")
@Getter @Setter @NoArgsConstructor
public class TaskGenerationJobConfig {
    private String domainShortName;
    private ReposSearcherConfig searcher;
    private ParserConfig parser;
    private GeneratorConfig generator;
    private ExporterConfig exporter;

    @Getter @Setter @NoArgsConstructor
    public static class ReposSearcherConfig {
        private String githubOAuthToken;
        private String outputFolderPath;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ParserConfig {
        private String pathToExecutable;
        private String outputFolderPath;
    }

    @Getter @Setter @NoArgsConstructor
    public static class GeneratorConfig {
        private String pathToExecutable;
        private String outputFolderPath;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ExporterConfig {
        private int StorageDummyDirsForNewFile;
        private String StorageUploadFilesBaseUrl ;
    }
}
