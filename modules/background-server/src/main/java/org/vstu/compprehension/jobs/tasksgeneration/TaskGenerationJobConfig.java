package org.vstu.compprehension.jobs.tasksgeneration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Configuration
@ConfigurationProperties(prefix = "task-generation")
@Getter @Setter @NoArgsConstructor
public class TaskGenerationJobConfig {
    private boolean runOnce;
    private String cronSchedule;
    private List<TaskConfig> tasks;

    @Getter @Setter @NoArgsConstructor
    public static class TaskConfig {
        private String domainShortName;
        private boolean enabled = true;
        private RunMode runMode = new RunMode.Incremental();
        private ReposSearcherConfig searcher;
        private ParserConfig parser;
        private GeneratorConfig generator;
        private List<CleanupMode> cleanupMode = List.of(new CleanupMode.CleanupDownloadedOlderThan(1, TimeUnit.DAYS), new CleanupMode.CleanupParsed());

        @Getter @Setter @NoArgsConstructor
        public static class ReposSearcherConfig {
            private boolean enabled = true;
            private String query = null;
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

    public interface CleanupMode {
        record CleanupDownloaded() implements CleanupMode {};
        record CleanupDownloadedShallow() implements CleanupMode {};
        record CleanupDownloadedOlderThan(long amount, TimeUnit unit) implements CleanupMode {};
        record CleanupParsed() implements CleanupMode {};
        record CleanupGenerated() implements CleanupMode {};

        @Component
        @ConfigurationPropertiesBinding
        public class ModeConverter implements Converter<String, CleanupMode> {

            @Override
            public CleanupMode convert(String from) {
                switch (from) {
                    case "CleanupDownloaded" -> {
                        return new CleanupDownloaded();
                    }
                    case "CleanupDownloadedShallow" -> {
                        return new CleanupDownloadedShallow();
                    }
                    case "CleanupParsed" -> {
                        return new CleanupParsed();
                    }
                    case "CleanupGenerated" -> {
                        return new CleanupGenerated();
                    }
                }

                if (from.startsWith("CleanupDownloadedOlderThan")) {
                    var pattern = Pattern.compile("^CleanupDownloadedOlderThan\\(\\s*(\\d+)\\s*,\\s*(SECONDS|MINUTES|HOURS|DAYS)\\s*\\)\\s*$", Pattern.CASE_INSENSITIVE);
                    var matcher = pattern.matcher(from);
                    if (matcher.find()) {
                        return new CleanupDownloadedOlderThan(Long.parseLong(matcher.group(1)), TimeUnit.valueOf(matcher.group(2).toUpperCase()));
                    }
                }

                throw new IllegalArgumentException("Unknown mode: " + from);
            }
        }
    }
}
