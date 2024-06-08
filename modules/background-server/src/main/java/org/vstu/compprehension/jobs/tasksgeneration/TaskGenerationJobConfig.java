package org.vstu.compprehension.jobs.tasksgeneration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

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
    private String cronSchedule;
    private RunMode runMode = new RunMode.Incremental();
    private EnumSet<CleanupMode> cleanupMode = EnumSet.of(CleanupMode.CleanupDownloadedOlderThanDay, CleanupMode.CleanupParsed);

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
    
    public interface RunMode {        
        public static record Full(int enoughQuestions) implements RunMode {};

        public static record Incremental() implements RunMode {};

        @Component
        @ConfigurationPropertiesBinding
        public class ModeConverter implements Converter<String, RunMode> {

            @Override
            public RunMode convert(String from) {
                String[] data = from.split(",");
                
                if (data[0].equalsIgnoreCase("full")) {                    
                    if (data.length > 1) {
                        return new Full(Integer.parseInt(data[1]));
                    }
                    return new Full(15_000);
                }
                
                if (data[0].equalsIgnoreCase("incremental")) {
                    return new Incremental();
                }
                
                throw new IllegalArgumentException("Unknown mode: " + from);
            }
        }
    }

    public enum CleanupMode {
        CleanupDownloaded,
        CleanupDownloadedShallow,
        CleanupDownloadedOlderThanDay,
        CleanupParsed,
        CleanupGenerated;
    }
}
