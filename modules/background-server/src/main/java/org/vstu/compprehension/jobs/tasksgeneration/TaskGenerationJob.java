package org.vstu.compprehension.jobs.tasksgeneration;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;
import org.jobrunr.jobs.annotations.Job;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.common.FileHelper;
import org.vstu.compprehension.common.StringHelper;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionRequestLogEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.models.repository.QuestionRequestLogRepository;
import org.vstu.compprehension.utils.FileUtility;
import org.vstu.compprehension.utils.ZipUtility;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Currently, for Expression domain only.
 */
@Log4j2
@Service
public class TaskGenerationJob {
    private final QuestionRequestLogRepository qrLogRep;
    private final QuestionMetadataRepository metadataRep;
    private final TaskGenerationJobConfig config;
    private final QuestionBank storage;

    @Autowired
    public TaskGenerationJob(QuestionRequestLogRepository qrLogRep, QuestionMetadataRepository metadataRep, TaskGenerationJobConfig config, QuestionBank storage) {
        this.qrLogRep = qrLogRep;
        this.metadataRep = metadataRep;
        this.config = config;
        this.storage = storage;
    }

    @Job(name = "task-generation-job", retries = 0)
    public void run() {
        log.info("Run generating questions for expression domain ...");
        try {
            switch (config.getRunMode()) {
                case TaskGenerationJobConfig.RunMode.Full full:
                    runImpl(full);
                    break;
                case TaskGenerationJobConfig.RunMode.Incremental incremental:
                    runImpl(incremental);
                    break;
                default:
                    throw new IllegalStateException("Unknown run mode: " + config.getRunMode());
            }
        } catch (Exception e) {
            log.error("job exception - {}", e.getMessage(), e);
            throw e;
        }
        if (config.isRunOnce()) {
            System.exit(0);
        }
    }
    
    private synchronized void runImpl(TaskGenerationJobConfig.RunMode.Full mode) {
        while (true) {
            var bankQuestionCount = metadataRep.countByDomainShortname(config.getDomainShortName());
            if (bankQuestionCount >= mode.enoughQuestions()) {
                log.info("Reached the limit of questions in the bank, finished job.");
                break;
            } else {
                log.info("More questions are needed in the bank (currently {}/{}, {} needed), continue job.", bankQuestionCount, mode.enoughQuestions(), mode.enoughQuestions() - bankQuestionCount);
            }

            // folders cleanup
            ensureFoldersCleaned();

            // download repositories
            var downloadedRepos = downloadRepositories();

            // do parsing
            var parsedRepos = parseRepositories(downloadedRepos);

            // do question generation
            var generatedRepos = generateQuestions(parsedRepos);

            // filter & save questions
            saveQuestions(generatedRepos);
        }
        
        log.info("completed");
    }
    
    private synchronized void runImpl(TaskGenerationJobConfig.RunMode.Incremental mode) {
        // TODO проверка на то, что нужны новые вопросы
        int enoughQuestionsAdded = QuestionBank.getQrEnoughQuestions(0);  // mark QRLog resolved if such many questions were added (e.g. 150)
        int tooFewQuestions      = QuestionBank.getTooFewQuestionsForQR(0); // (e.g. 50)
        var qrLogsToProcess      = qrLogRep.findAllNotProcessed(config.getDomainShortName(), tooFewQuestions);

        if (qrLogsToProcess.isEmpty()) {
            log.info("Nothing to process, finished job.");
            return;
        }

        log.info("QR logs to process: {}", qrLogsToProcess.size());
        log.info("QR log ids to be processed: {}.", qrLogsToProcess.stream().map(QuestionRequestLogEntity::getId).collect(Collectors.toList()));

        // folders cleanup
        ensureFoldersCleaned();

        // download repositories
        var downloadedRepos = downloadRepositories();

        // do parsing
        var parsedRepos = parseRepositories(downloadedRepos);

        // do question generation
        var generatedRepos = generateQuestions(parsedRepos);

        // filter & save questions
        saveQuestions(generatedRepos, qrLogsToProcess, enoughQuestionsAdded);

        log.info("completed");
    }

    @SneakyThrows
    private void ensureFoldersCleaned() {
        var cleanupModes = config.getCleanupMode();
        if (cleanupModes.isEmpty()) {
            log.info("no folders cleanup needed");
            return;
        }

        val downloadedPath = Path.of(config.getSearcher().getOutputFolderPath());
        val downloadedPathExists = Files.exists(downloadedPath);
        if (cleanupModes.contains(TaskGenerationJobConfig.CleanupMode.CleanupDownloadedOlderThanDay) && downloadedPathExists) {
            try (var paths = Files.list(downloadedPath)) {
                for (var path : paths.collect(Collectors.toSet())) {
                    var file = path.toFile();
                    if (file.isDirectory() && file.lastModified() < LocalDateTime.now(ZoneId.of("UTC")).minusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()) {
                        FileUtils.deleteDirectory(file);
                        continue;
                    }
                    if (file.isDirectory()) {
                        FileUtility.clearDirectory(path);
                        continue;
                    }
                    if (file.isFile()) {
                        var deleted = file.delete();
                        if (!deleted) {
                            log.error("Failed to delete file: {}", file.getAbsolutePath());
                        }
                    }
                }
            } catch (Exception exception) {
                log.error("Error while clearing downloaded folders: `{}`", exception.getMessage(), exception);
            }            
            log.info("successfully cleanup downloaded repos folder (older than 1 day)");
        } else if (cleanupModes.contains(TaskGenerationJobConfig.CleanupMode.CleanupDownloadedShallow) && downloadedPathExists) {
            try (var paths = Files.list(downloadedPath)) {
                for (var path : paths.collect(Collectors.toSet())) {
                    var file = path.toFile();
                    if (file.isDirectory()) {
                        FileUtility.clearDirectory(path);
                        continue;
                    }
                    if (file.isFile()) {
                        var deleted = file.delete();
                        if (!deleted) {
                            log.error("Failed to delete file: {}", file.getAbsolutePath());
                        }
                    }
                }
            } catch (Exception exception) {
                log.error("Error while clearing downloaded folders: `{}`", exception.getMessage(), exception);
            }
            log.info("successfully cleanup downloaded repos folder (shallow mode)");
        } else if (cleanupModes.contains(TaskGenerationJobConfig.CleanupMode.CleanupDownloaded) && downloadedPathExists) {
            try {
                FileUtils.deleteDirectory(downloadedPath.toFile());
            } catch (IllegalArgumentException exception) {
                log.error("Error while clearing downloaded folders: `{}`", exception.getMessage(), exception);
            }
            log.info("successfully cleanup downloaded repos folder");
        }

        if (cleanupModes.contains(TaskGenerationJobConfig.CleanupMode.CleanupParsed)) {
            val parsedPath = Path.of(config.getParser().getOutputFolderPath());
            if (Files.exists(parsedPath))
                FileHelper.deleteFolderContent(parsedPath.toFile());
            log.info("successfully cleanup parsed questions folder");
        }

        if (cleanupModes.contains(TaskGenerationJobConfig.CleanupMode.CleanupGenerated)) {
            val generatedPath = Path.of(config.getGenerator().getOutputFolderPath());
            if (Files.exists(generatedPath))
                FileHelper.deleteFolderContent(generatedPath.toFile());
            log.info("successfully cleanup generated questions folder");
        }

        log.info("cleanup finished");
    }

    @SneakyThrows
    private List<Path> downloadRepositories() {
        var downloaderConfig = config.getSearcher();

        if (!downloaderConfig.isEnabled()) {
            // get all downloaded previously:
            var rootDir = Path.of(downloaderConfig.getOutputFolderPath());
            try (var list = Files.list(rootDir)) {
                return list.filter(Files::isDirectory).collect(Collectors.toList());
            }
        }

        // ensure out folder exists
        var outputFolderPath = Path.of(downloaderConfig.getOutputFolderPath());
        Files.createDirectories(outputFolderPath);

        // Учесть историю по использованным репозиториям
        var seenReposNames = metadataRep.findAllOrigins(config.getDomainShortName(), LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now(ZoneId.of("UTC")).minusDays(1));
        if (downloaderConfig.isSkipDownloadedRepositories()) {
            // add repo names (on disk) to seenReposNames
            try (var list = Files.list(outputFolderPath)) {
                var repos = list.filter(Files::isDirectory).map(Path::getFileName).map(Path::toString).toList();
                seenReposNames.addAll(repos);
            }            
        }

        // github limits amount of returnable searchable repositories to 1000, so we create 3 queries with different sorting
        // 1) ordered by stars (most popular first)
        // 2) ordered by updated date (newest first)
        // 3) ordered by forks (most forks first)
        // pagesize == 100 is max per query
        GitHub github = new GitHubBuilder()
                .withOAuthToken(downloaderConfig.getGithubOAuthToken())
                .withRateLimitChecker(new RateLimitChecker.LiteralValue(20), RateLimitTarget.SEARCH)
                .build();
        var repoSearchQueries = new ArrayList<PagedSearchIterable<GHRepository>>(3);
        repoSearchQueries.add(github.searchRepositories()
                .language("c")
                .size("50..100000")
                .fork(GHFork.PARENT_ONLY)
                .sort(GHRepositorySearchBuilder.Sort.STARS)
                .order(GHDirection.DESC)
                .list()
                .withPageSize(100));        
        repoSearchQueries.add(github.searchRepositories()
                .language("c")
                .size("50..100000")
                .fork(GHFork.PARENT_ONLY)
                .sort(GHRepositorySearchBuilder.Sort.UPDATED)
                .order(GHDirection.DESC)
                .list()
                .withPageSize(100));
        repoSearchQueries.add(github.searchRepositories()
                .language("c")
                .size("50..100000")
                .fork(GHFork.PARENT_ONLY)
                .sort(GHRepositorySearchBuilder.Sort.FORKS)
                .order(GHDirection.DESC)
                .list()
                .withPageSize(100));
        
        var downloadedRepos = new ArrayList<Path>();
        int skipped         = 0;
        for (var repoSearchQuery : repoSearchQueries) {
            for (var repo : repoSearchQuery) {
                if (seenReposNames.contains(repo.getName())) {
                    skipped++;
                    log.printf(Level.INFO, "Skip processed GitHub repo [%3d]: %s", skipped, repo.getName());
                    continue;
                }
                log.info("Downloading repo [{}] ...", repo.getFullName());

                repo.readZip(s -> {
                    Path zipFile = Path.of(downloaderConfig.getOutputFolderPath(), repo.getName() + ".zip")
                            .toAbsolutePath();
                    Path targetFolderPath = Path.of(downloaderConfig.getOutputFolderPath(), repo.getName())
                            .toAbsolutePath();
                    Files.createDirectories(targetFolderPath);
                    Files.copy(s, zipFile, StandardCopyOption.REPLACE_EXISTING);
                    ZipUtility.unzip(zipFile.toString(), targetFolderPath.toString());
                    Files.delete(zipFile);
                    downloadedRepos.add(targetFolderPath);
                    log.info("Downloaded repo [{}] to location [{}]", repo.getFullName(), targetFolderPath);
                    return 0;
                }, null);

                // for now, limited number of repositories
                if (downloadedRepos.size() >= downloaderConfig.getRepositoriesToDownload())
                    return downloadedRepos;
            }

            log.info("No enough repositories found for query. {}/{} repositories downloaded so far. Trying the next query...", downloadedRepos.size(), downloaderConfig.getRepositoriesToDownload());
            
            // throttle            
            Thread.sleep(2000L);
        }
        
        return downloadedRepos;
    }

    @SneakyThrows
    private List<Path> parseRepositories(List<Path> downloadedRepos) {
        var parserConfig = config.getParser();
        var outputFolderPath = Path.of(parserConfig.getOutputFolderPath()).toAbsolutePath();
        if (!parserConfig.isEnabled()) {
            log.info("parser is disabled by config");
            try (var list = Files.list(outputFolderPath)) {
                return list.filter(Files::isDirectory).collect(Collectors.toList());
            }
        }

        // ensure output folder exists
        Files.createDirectories(outputFolderPath);

        var result = new ArrayList<Path>(downloadedRepos.size());
        for (var repo : downloadedRepos) {
            log.info("Start parsing sources for repo [{}]", repo);

            String leafFolder = repo.getFileName().toString();
            Path destination = Path.of(outputFolderPath.toString(), leafFolder);
            Files.createDirectories(destination);
            result.add(destination);

            var files = FileUtility.findFiles(repo, new String[]{".c", ".m"});
            files = files.subList(0, Math.min(50, files.size()));
            log.info("Found {} *.c & *.m files", files.size());

            // TODO: make parser cmd customizable?

            List<String> parserProcessCommandBuilder = new ArrayList<>();
            parserProcessCommandBuilder.add(parserConfig.getPathToExecutable());
            parserProcessCommandBuilder.addAll(files);

            // reduce number of arguments on command line if necessary
            // TODO: loop over batches if required to split the task
            parserProcessCommandBuilder = FileUtility.truncateLongCommandline(parserProcessCommandBuilder, 2 + 10 + 5 + destination.toString().length());

            parserProcessCommandBuilder.add("--");
            parserProcessCommandBuilder.add(config.getDomainShortName());  // e.g. "expression"
            parserProcessCommandBuilder.add(destination.toString());
            log.debug("Parser executable command: {}", parserProcessCommandBuilder);

            try {
                log.info("Run parser on repo: [{}]", leafFolder);
                try {
                    Files.createDirectories(destination);
                } catch (AccessDeniedException ignored) {}
                var parserProcess = new ProcessBuilder(parserProcessCommandBuilder)
                        .redirectErrorStream(true)
                        .start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(parserProcess.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) { // do not remove this cycle! waitFor wouldn't work without it
                    log.debug("parser's stdout: {}", line);
                }
                parserProcess.waitFor(10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.warn("Parsing timeout exception", e);
            } catch (Exception e) {
                log.warn("Parsing exception", e);
            }

            log.info("Repo [{}] parsed", repo);
        }

        return result;
    }

    @SneakyThrows
    private List<Path> generateQuestions(List<Path> parsedRepos) {
        var generatorConfig = config.getGenerator();
        if (!generatorConfig.isEnabled()) {
            log.info("generator is disabled by config");
            try (var list = Files.list(Path.of(generatorConfig.getOutputFolderPath()))) {
                return list.filter(Files::isDirectory).collect(Collectors.toList());
            }
        }

        // find all sub-folders in root directory of parser output directory
        log.info("Start question generation from {} repository(-ies) ...", parsedRepos.size());

        var result = new ArrayList<Path>(parsedRepos.size());
        for (var repoDir : parsedRepos) {
            var allTtlFiles = FileUtility.findFiles(repoDir, new String[]{".ttl"});
            log.info("Found {} ttl files in: {}", allTtlFiles.size(), repoDir);

            String leafFolder = repoDir.getFileName().toString();
            Path destination = Path.of(generatorConfig.getOutputFolderPath(), leafFolder);
            Files.createDirectories(destination);
            result.add(destination);

            List<String> cmd = new ArrayList<>();
            if (generatorConfig.getPathToExecutable().endsWith(".bat") || generatorConfig.getPathToExecutable().endsWith(".sh"))
                cmd.add(generatorConfig.getPathToExecutable());
            else
                cmd.addAll(Arrays.stream(generatorConfig.getPathToExecutable().split("\\s")).toList());
            cmd.add("--source");
            cmd.add(String.valueOf(repoDir));
            cmd.add("--output");
            cmd.add(String.valueOf(destination));
            cmd.add("--sourceId");
            cmd.add(leafFolder);

            // TODO: call specific generation tool
            try {
                log.info("Run generator on repo: [{}]", leafFolder);
                log.debug("Shell command to run: [{}]", cmd);
                Files.createDirectories(destination);
                var parserProcess = new ProcessBuilder(cmd)
                        .redirectErrorStream(true)
                        .start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(parserProcess.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) { // do not remove this cycle! waitFor wouldn't work without it
                    log.debug("generator's stdout: {}", line);
                }
                parserProcess.waitFor(10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.warn("Question generation timeout exception", e);
            } catch (Exception e) {
                log.warn("Question generation exception", e);
            }
        }

        return result;
    }

    @SneakyThrows
    private void saveQuestions(List<Path> generatedRepos) {
        saveQuestions(generatedRepos, null, -1);
    }

    @SneakyThrows
    private void saveQuestions(List<Path> generatedRepos, @Nullable List<QuestionRequestLogEntity> qrLogsToProcess, int enoughQuestionsForEachQr) {
        var generatorConfig = config.getGenerator();
        var exportConfig = config.getExporter();
        if (!generatorConfig.isEnabled()) {
            log.info("generator is disabled by config");
            return;
        }

        log.info("Start saving questions generated from {} repositories ...", generatedRepos.size());

        for (var repoDir : generatedRepos) {
            String repoName = repoDir.getFileName().toString();
            log.info("Start processing repo [{}]", repoName);

            // загрузить из папки полученные вопросы
            var allJsonFiles = FileUtility.findFiles(repoDir, new String[]{".json"});
            log.info("Found {} json files in: {}", allJsonFiles.size(), repoDir);

            int savedQuestions = 0;
            int skippedQuestions = 0; // loaded but not kept since not required by any QR

            var domainId = "ProgrammingLanguageExpressionDomain";
            Set<QuestionRequestLogEntity> qrLogsProcessed = new HashSet<>();

            for (val file : allJsonFiles) {
                val q = SerializableQuestion.deserialize(file);
                if (q == null) {
                    continue;
                }

                QuestionMetadataEntity meta = q.toMetadataEntity();
                if (meta == null) {
                    skippedQuestions += 1;
                    // в вопросе нет метаданных, невозможно проверить
                    log.warn("[info] cannot save question which does not contain metadata. Source file: {}", file);
                    continue;
                }
                
                if (metadataRep.existsByNameOrTemplateId(config.getDomainShortName(), meta.getName(), meta.getTemplateId())) {
                    skippedQuestions += 1;
                    log.info("Template [{}] or question [{}] already exists. Skipping...", meta.getTemplateId(), meta.getName());
                    continue;
                }

                // init container: list of QR logs requiring this question
                meta.setQrlogIds(new ArrayList<>());

                // Проверить, подходит ли он нам
                // если да, то сразу импортировать его в боевой банк, создав запись метаданных, записав в них информацию о затребовавших QR-логах, и скопировав файл с данными вопроса...
                boolean shouldSave = false;
                if (qrLogsToProcess == null) {
                    shouldSave = true;
                } else {
                    for (val qr : qrLogsToProcess) {
                        if (!storage.isMatch(meta, qr)) {
                            log.debug("Question [{}] does not match qr {}", q.getQuestionData().getQuestionName(), qr.getId());
                            continue;
                        }

                        log.debug("Question [{}] matches qr {}", q.getQuestionData().getQuestionName(), qr.getId());

                        // set flag to use this question
                        shouldSave = true;
                        // update question metrics
                        meta.getQrlogIds().add(qr.getId());
                        // update qr metrics
                        qr.setAddedQuestions(Optional.ofNullable(qr.getAddedQuestions()).orElse(0) + 1);
                        qrLogsProcessed.add(qr);
                    }
                }

                if (!shouldSave) {
                    skippedQuestions += 1;
                    log.debug("Question [{}] skipped because zero qr matches", meta.getName());
                    continue;
                }

                // copy data file and save local sub-path to it
                String qDataPath = StringHelper.isNullOrEmpty(exportConfig.getStorageUploadRelativePath())
                        ? storage.saveQuestionData(domainId, q.getQuestionData().getQuestionName(), q)
                        : storage.saveQuestionData(domainId, exportConfig.getStorageUploadRelativePath(), q.getQuestionData().getQuestionName(), q);
                meta.setQDataGraph(qDataPath);
                meta.setDateCreated(new Date());
                meta.setUsedCount(0L);

                if (generatorConfig.isSaveToDb()) {
                    meta = storage.saveMetadataEntity(meta);
                } else {
                    log.info("Saving updates to DB actually SKIPPED due to DEBUG mode:");
                }
                log.info("* * *");
                log.info("Question [{}] saved with path [{}]. Metadata id: {}, Affected qrs: {}", q.getQuestionData().getQuestionName(), qDataPath, meta.getId(), meta.getQrlogIds());
                savedQuestions += 1;
            }

            log.info("Skipped {} questions of {} generated.", skippedQuestions, allJsonFiles.size());
            log.info("Saved {} questions of {} generated.", savedQuestions, allJsonFiles.size());

            if (qrLogsProcessed.size() > 0) {

                // update QR-log: statistics and possibly status
                for (var qr : qrLogsProcessed) {

                    // increment processed counter for each repository touched by this qr
                    qr.setProcessedCount(qr.getProcessedCount() + 1);
                    if (qr.getFoundCount() + qr.getAddedQuestions() >= enoughQuestionsForEachQr) {
                        // don't more need to process it.
                        qr.setOutdated(1);
                        log.info("Question-request-log (id: {}) resolved with {} questions added.", qr.getId(), qr.getAddedQuestions());
                    }
                    qr.setLastProcessedDate(new Date());
                }
                if (generatorConfig.isSaveToDb()) {
                    qrLogRep.saveAll(qrLogsProcessed);
                } else {
                    log.info("Saving updates actually SKIPPED due to DEBUG mode.");
                }

                log.info("saved updates to {} question-request-log rows.", qrLogsProcessed.size());
            }
        }
    }
}
