package org.vstu.compprehension.jobs.tasksgeneration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.EnumSet;

@Configuration
@ConfigurationProperties(prefix = "task-generation")
@Getter @Setter @NoArgsConstructor
public class TaskGenerationJobConfig {
    private String domainShortName;
    private ReposSearcherConfig searcher;
    private ParserConfig parser;
    private GeneratorConfig generator;
    private ExporterConfig exporter;
    private boolean runOnce;
    private int intervalMinutes;
    private EnumSet<CleanupMode> cleanupMode = EnumSet.of(CleanupMode.CleanupDownloadedShallow, CleanupMode.CleanupParsed);

    @Getter @Setter @NoArgsConstructor
    public static class ReposSearcherConfig {
        private boolean enabled = true;
        private String githubOAuthToken;
        private String outputFolderPath;
        private boolean skipDownloadedRepositories = true;
        private int repositoriesToDownload = 1;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ParserConfig {
        private boolean enabled = true;
        private String pathToExecutable;
        private String outputFolderPath;
    }

    @Getter @Setter @NoArgsConstructor
    public static class GeneratorConfig {
        private boolean enabled = true;
        private String pathToExecutable;
        private String outputFolderPath;
        private boolean saveToDb = true;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ExporterConfig {
        private int storageDummyDirsForNewFile = 2;
        private URI storageUploadFilesBaseUrl;
        private @Nullable String storageUploadRelativePath = "q_data";
    }

    public static enum CleanupMode {
        CleanupDownloaded,
        CleanupDownloadedShallow,
        CleanupParsed,
        CleanupGenerated;
    }
}
