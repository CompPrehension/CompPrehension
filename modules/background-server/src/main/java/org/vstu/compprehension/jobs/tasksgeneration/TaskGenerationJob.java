package org.vstu.compprehension.jobs.tasksgeneration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jobrunr.jobs.annotations.Job;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.common.BatchingIterator;
import org.vstu.compprehension.common.FileHelper;
import org.vstu.compprehension.dto.GenerationRequest;
import org.vstu.compprehension.dto.GenerationRequestGroup;
import org.vstu.compprehension.models.businesslogic.SourceCodeRepositoryInfo;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestionTemplate;
import org.vstu.compprehension.models.entities.QuestionDataEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.QuestionGenerationRequestRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.utils.FileUtility;
import org.vstu.compprehension.utils.ZipUtility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Currently, for Expression domain only.
 */
@Log4j2
@Service
public class TaskGenerationJob {
    private final QuestionMetadataRepository metadataRep;
    private final QuestionGenerationRequestRepository generatorRequestsQueue;
    private final TaskGenerationJobConfig tasks;
    private final QuestionBank storage;
    private static RepositoriesCrawler repositories; // TODO: make it non-static

    @Autowired
    public TaskGenerationJob(QuestionMetadataRepository metadataRep, QuestionGenerationRequestRepository generatorRequestsQueue, TaskGenerationJobConfig tasks, QuestionBank storage) {
        this.metadataRep = metadataRep;
        this.generatorRequestsQueue = generatorRequestsQueue;
        this.tasks = tasks;
        this.storage = storage;
    }

    @Job(name = "task-generation-job", retries = 0)
    public void run() {
        log.info("Run generating questions for expression domain ...");

        for (val config : tasks.getTasks())
        {
            if (!config.isEnabled())
                continue;

            log.info("Run generating questions for {} domain ...", config.getDomainShortName());

            try {
                runWithMode(config);
            } catch (Exception e) {
                log.error("job exception - {} | {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }
    
    private void runWithMode(TaskGenerationJobConfig.TaskConfig config) {
        switch (config.getRunMode()) {
            case TaskGenerationJobConfig.RunMode.Full full:
                runImpl(config, full);
                break;
            case TaskGenerationJobConfig.RunMode.Incremental incremental:
                runImpl(config, incremental);
                break;
            default:
                throw new IllegalStateException("Unknown run mode: " + config.getRunMode());
        };
    }

    private synchronized void runImpl(TaskGenerationJobConfig.TaskConfig config, TaskGenerationJobConfig.RunMode.Full mode) {
        while (true) {
            var bankQuestionCount = metadataRep.countByDomainShortname(config.getDomainShortName());
            if (bankQuestionCount >= mode.enoughQuestions()) {
                log.info("Reached the limit of questions in the bank, finished job.");
                break;
            } else {
                log.info("More questions are needed in the bank (currently {}/{}, {} needed), continue job.", bankQuestionCount, mode.enoughQuestions(), mode.enoughQuestions() - bankQuestionCount);
            }

            // folders cleanup
            ensureFoldersCleaned(config);

            // download repositories
            var downloadedRepos = downloadRepositories(config);

            // do parsing
            var parsedRepos = parseRepositories(config, downloadedRepos);

            // do question generation
            var generatedRepos = generateQuestions(config, parsedRepos);

            // filter & save questions
            saveQuestions(config, generatedRepos);
        }
    }
    
    private synchronized void runImpl(TaskGenerationJobConfig.TaskConfig config, TaskGenerationJobConfig.RunMode.Incremental mode) {
        var incrementalTriesCount = 0;
        while (true) {
            var startTime = System.currentTimeMillis();

            var generationRequests = getGenerationRequests(config.getDomainShortName());

            if (generationRequests.isEmpty()) {
                log.info("No generation requests found. Finish job");
                break;
            }
            if (incrementalTriesCount++ >= 50) {
                log.info("Too many unsuccessful incremental generation. Finish job");
                break;
            }

            var generationRequestIds = generationRequests.stream()
                    .map(GenerationRequestGroup::getGenerationRequestIds)
                    .collect(Collectors.toList());
            log.debug("Generation requests ids: {}", generationRequestIds);

            // folders cleanup
            ensureFoldersCleaned(config);

            // download repositories
            var downloadedRepos = downloadRepositories(config);

            // do parsing
            var parsedRepos = parseRepositories(config, downloadedRepos);

            // do question generation
            var generatedRepos = generateQuestions(config, parsedRepos);

            // filter & save questions
            var endTime = System.currentTimeMillis();
            if (endTime - startTime > 10_000) {
                log.info("Re-fetching generation requests");

                generationRequests = getGenerationRequests(config.getDomainShortName());
                if (generationRequests.isEmpty()) {
                    log.info("No generation requests found. Finish job");
                    break;
                }
            }

            saveQuestions(config, generatedRepos, generationRequests);
        }
    }

    private List<GenerationRequestGroup> getGenerationRequests(String domainShortName) {
        var requests = generatorRequestsQueue.findAllActual(domainShortName, LocalDateTime.now().minusMonths(3));

        if (!requests.isEmpty()) {
            log.info("Found {} generation requests", requests.size());

            if (requests.size() > 5_000) {
                requests = requests.subList(0, 5_000);
                log.info("Too many generation requests found. Limiting to 5000.");
            }
        }

        return requests;
    }

    @SneakyThrows
    private void ensureFoldersCleaned(TaskGenerationJobConfig.TaskConfig config) {
        var cleanupModes = config.getCleanupMode();
        if (cleanupModes.isEmpty()) {
            log.info("no folders cleanup needed");
            return;
        }

        val downloadedPath = Path.of(config.getSearcher().getOutputFolderPath());
        val downloadedPathExists = Files.exists(downloadedPath);

        log.info("Cleanup modes: {}", cleanupModes);
        for (var mode : cleanupModes) {
            switch (mode) {
                case TaskGenerationJobConfig.CleanupMode.CleanupDownloadedOlderThan(long amount, TimeUnit timeUnit) when downloadedPathExists -> {
                    try (var paths = Files.list(downloadedPath)) {
                        for (var path : paths.collect(Collectors.toSet())) {
                            var file = path.toFile();
                            if (file.isDirectory() && file.lastModified() < LocalDateTime.now(ZoneId.of("UTC")).minus(amount, timeUnit.toChronoUnit()).toInstant(ZoneOffset.UTC).toEpochMilli()) {
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
                    log.info("successfully cleanup downloaded repos folder (older than {} {})", amount, timeUnit);
                }
                case TaskGenerationJobConfig.CleanupMode.CleanupDownloadedShallow() when downloadedPathExists -> {
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
                }
                case TaskGenerationJobConfig.CleanupMode.CleanupDownloaded() -> {
                    try {
                        FileUtils.deleteDirectory(downloadedPath.toFile());
                    } catch (IllegalArgumentException exception) {
                        log.error("Error while clearing downloaded folders: `{}`", exception.getMessage(), exception);
                    }
                    log.info("successfully cleanup downloaded repos folder");
                }
                case TaskGenerationJobConfig.CleanupMode.CleanupParsed() -> {
                    val parsedPath = Path.of(config.getParser().getOutputFolderPath());
                    if (Files.exists(parsedPath))
                        FileHelper.deleteFolderContent(parsedPath.toFile());
                    log.info("successfully cleanup parsed questions folder");
                }
                case TaskGenerationJobConfig.CleanupMode.CleanupGenerated() -> {
                    val generatedPath = Path.of(config.getGenerator().getOutputFolderPath());
                    if (Files.exists(generatedPath))
                        FileHelper.deleteFolderContent(generatedPath.toFile());
                    log.info("successfully cleanup generated questions folder");
                }
                case null, default -> {
                    log.info("Unknown cleanup mode: {}", mode);
                }
            }
        }

        log.info("cleanup finished");
    }

    @SneakyThrows
    private List<Path> downloadRepositories(TaskGenerationJobConfig.TaskConfig config) {
        if (repositories == null) {
            repositories = new RepositoriesCrawler(config.getSearcher().getGithubOAuthToken(), 10_000,
                    config.getSearcher().getQuery());
        }

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

        // Учесть историю по полностью использованным репозиториям + загруженным недавно -- игнорируем их
        // TODO временно для эксперимента используем только ни разу не обработанные за 24ч репозитории
        var seenReposNames = metadataRep.findProcessedOrigins(config.getDomainShortName(), LocalDateTime.now().minusHours(24))
            .stream().map(s -> s.replaceAll("/", "_"))
            .collect(Collectors.toSet());
        if (downloaderConfig.isSkipDownloadedRepositories()) {
            // add repo names (on disk) to seenReposNames
            try (var list = Files.list(outputFolderPath)) {
                var repos = list.filter(Files::isDirectory).map(Path::getFileName).map(Path::toString).toList();
                seenReposNames.addAll(repos);
            }
        }

        var downloadedRepos = new ArrayList<Path>();
        int skipped         = 0;
        try (ExecutorService executorService = Executors.newFixedThreadPool(1)) {
            for (var repo : repositories) {
                var repoId = repo.getFullName().replaceAll("/", "_");
                if (seenReposNames.contains(repoId)) {
                    skipped++;
                    log.printf(Level.INFO, "Skip processed GitHub repo [%3d]: %s", skipped, repo.getFullName());
                    continue;
                }

                log.info("Downloading repo [{}] ...", repo.getFullName());

                Path zipFile = Path.of(downloaderConfig.getOutputFolderPath(), repo.getName() + ".zip").toAbsolutePath();
                Path targetFolderPath = Path.of(downloaderConfig.getOutputFolderPath(), repoId).toAbsolutePath();

                Future<Void> future = executorService.submit(() -> {
                    try {
                        Files.createDirectories(targetFolderPath);

                        repo.readZip(s -> {
                            try {
                                Files.copy(s, zipFile, StandardCopyOption.REPLACE_EXISTING);
                                ZipUtility.unzip(zipFile.toString(), targetFolderPath.toString());
                                Files.delete(zipFile);
                                downloadedRepos.add(targetFolderPath);
                                SourceCodeRepositoryInfo info = SourceCodeRepositoryInfo.builder()
                                        .name(repo.getFullName())
                                        .license(repo.getLicense() != null
                                            ? repo.getLicense().getName()
                                            : null)
                                        .url(repo.getHtmlUrl().toString())
                                        .build();
                                Gson gson = new GsonBuilder()
                                        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                                        .disableHtmlEscaping()
                                        .setPrettyPrinting()
                                        .create();
                                Files.createFile(Path.of(targetFolderPath.toString(), ".gh_repo_info"));
                                Files.writeString(Path.of(targetFolderPath.toString(), ".gh_repo_info"),
                                        gson.toJson(info), StandardOpenOption.WRITE);
                                log.info("Downloaded repo [{}] to location [{}]", repo.getFullName(), targetFolderPath);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return 0;
                        }, null);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        // Cleanup in case of an exception or interruption
                        if (Files.exists(zipFile)) {
                            try {
                                Files.delete(zipFile);
                            } catch (IOException e) {
                                log.error("Failed to delete zip file [{}]", zipFile, e);
                            }
                        }
                    }
                    return null;
                });

                try {
                    future.get(45, TimeUnit.SECONDS);  // Timeout after 45 seconds
                } catch (TimeoutException e) {
                    log.warn("Timeout while downloading repo [{}] from GitHub", repo.getFullName());
                    future.cancel(true);  // Cancel the task if it times out
                } catch (Exception e) {
                    log.error("Error while downloading repo [{}] from GitHub. {}", repo.getFullName(), e.getMessage(), e);
                }

                if (downloadedRepos.size() >= downloaderConfig.getRepositoriesToDownload()) {
                    log.info("Downloaded enough repositories. {}/{} repositories downloaded so far.", downloadedRepos.size(), downloaderConfig.getRepositoriesToDownload());
                    break;
                }

                // throttle
                Thread.sleep(2000L);
                log.debug("Sleeped {}ms to avoid github api abuse", 2000);
            }
        }
        
        return downloadedRepos;
    }

    /** TODO: delete after compph-task-generator.exe has its input interface fixed. */
    private String fixDomainShortName(String name) {
        if (name.equals("ctrl_flow"))
            name = "control_flow";

        return name;
    }

    @SneakyThrows
    private List<Path> parseRepositories(TaskGenerationJobConfig.TaskConfig config, List<Path> downloadedRepos) {
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

            String[] supportedFilenames;
            if (config.getDomainShortName().equals("expression_dt")) {
                supportedFilenames = new String[]{".c", ".cpp", ".py", ".java", ".h", ".hpp", ".cxx"};
            } else {
                supportedFilenames = new String[] {".c", ".m"};
            }
            var files = FileUtility.findFiles(repo, supportedFilenames);
            files = files.subList(0, Math.min(50, files.size()));
            log.info("Found {} source code files", files.size());

            // TODO: make parser cmd customizable?

            List<String> parserProcessCommandBuilder = new ArrayList<>();
            if (parserConfig.getPathToExecutable().endsWith(".bat") || parserConfig.getPathToExecutable().endsWith(".sh"))
                parserProcessCommandBuilder.add(parserConfig.getPathToExecutable());
            else
                parserProcessCommandBuilder.addAll(Arrays.stream(parserConfig.getPathToExecutable().split("\\s")).toList());
            parserProcessCommandBuilder.addAll(files);

            // reduce number of arguments on command line if necessary
            // TODO: loop over batches if required to split the task
            parserProcessCommandBuilder = FileUtility.truncateLongCommandline(parserProcessCommandBuilder, 2 + 10 + 5 + destination.toString().length());

            parserProcessCommandBuilder.add("--");
            parserProcessCommandBuilder.add(Path.of(repo.toString(), ".gh_repo_info").toString());  // e.g. "expression"
            parserProcessCommandBuilder.add(fixDomainShortName(config.getDomainShortName()));  // e.g. "expression"
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
    private List<Path> generateQuestions(TaskGenerationJobConfig.TaskConfig config, List<Path> parsedRepos) {
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
            var allFiles = FileUtility.findFiles(repoDir, new String[]{".ttl", ".json"});
            log.info("Found {} question files in: {}", allFiles.size(), repoDir);

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
            cmd.add("--limit");
            cmd.add(String.valueOf(3000)); // TODO для исправления затупов на больших репозиториях
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
    private void saveQuestions(TaskGenerationJobConfig.TaskConfig config, List<Path> generatedRepos) {
        saveQuestions(config, generatedRepos, null);
    }

    @SneakyThrows
    private void saveQuestions(TaskGenerationJobConfig.TaskConfig config, List<Path> generatedRepos, @Nullable List<GenerationRequestGroup> generationRequests) {
        var generatorConfig = config.getGenerator();
        if (!generatorConfig.isEnabled()) {
            log.info("generator is disabled by config");
            return;
        }

        log.info("Start saving questions generated from {} repositories ...", generatedRepos.size());

        var questionsGenerated = new HashMap<GenerationRequest, Integer>();
        var incompletedRequests = new HashMap<GenerationRequestGroup, HashSet<GenerationRequest>>();
        for (var gr : (generationRequests == null ? List.<GenerationRequestGroup>of() : generationRequests)) {
            incompletedRequests.put(gr, Arrays.stream(gr.getGenerationRequests()).collect(Collectors.toCollection(HashSet::new)));
        }

        for (var repoDir : generatedRepos) {
            String repoName = repoDir.getFileName().toString();
            log.info("Start processing repo [{}]", repoName);

            var allJsonFiles = FileUtility.findFiles(repoDir, new String[]{".json"});
            log.info("Found {} json files in: {}", allJsonFiles.size(), repoDir);

            AtomicInteger processed = new AtomicInteger(0);
            AtomicInteger savedQuestions = new AtomicInteger();
            AtomicInteger skippedQuestions = new AtomicInteger(); // loaded but not kept since not required by any QR
            AtomicInteger existingQuestions = new AtomicInteger();

            AtomicInteger matchesRequest = new AtomicInteger();

            var allJsonBatches = BatchingIterator.batchedStreamOf(
                    allJsonFiles.stream()
                        .map(SerializableQuestionTemplate::deserialize)
                        .filter(Objects::nonNull),
                    1000);
            allJsonBatches.forEach(batch -> {

                var questionNames = batch.stream()
                        .flatMap(q -> q.getMetadataList().stream())
                        .map(SerializableQuestionTemplate.QuestionMetadata::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                var existingQuestionNames = metadataRep.findExistingNames(config.getDomainShortName(), questionNames);

                var templateIds = batch.stream()
                        .flatMap(q -> q.getMetadataList().stream())
                        .map(SerializableQuestionTemplate.QuestionMetadata::getTemplateId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                var existingTemplateIds = metadataRep.findExistingTemplateIds(config.getDomainShortName(), templateIds);
                var metadataToSave = new ArrayList<QuestionMetadataEntity>(batch.size());

                for (var q : batch) {
                    HashSet<QuestionMetadataEntity> metaList = q.getMetadataList().stream()
                            .map(SerializableQuestionTemplate.QuestionMetadata::toMetadataEntity)
                            .collect(Collectors.toCollection(HashSet::new));
                    if (metaList.isEmpty()) {
                        skippedQuestions.addAndGet(1);
                        // в вопросе нет метаданных, невозможно проверить
                        log.warn("[info] cannot save question which does not contain metadata. Question name: {}", q.getCommonQuestion().getQuestionData().getQuestionName());
                        continue;
                    }

                    var metadataToRemove = new ArrayList<QuestionMetadataEntity>();
                    for (QuestionMetadataEntity meta : metaList) {
                        if (existingQuestionNames.contains(meta.getName()) || existingTemplateIds.contains(meta.getTemplateId())) {                            
                            log.debug("Template [{}] or question [{}] already exists. Skipping...", meta.getTemplateId(), meta.getName());
                            metadataToRemove.add(meta);
                        }
                    }
                    metadataToRemove.forEach(metaList::remove);
                    if (metaList.isEmpty()) {
                        log.debug("All metadata already exists for question [{}]. Skipping...", q.getCommonQuestion().getQuestionData().getQuestionName());
                        existingQuestions.addAndGet(1);
                        skippedQuestions.addAndGet(1);
                        continue;
                    }

                    // Проверить, подходит ли он нам
                    // если да, то сразу импортировать его в боевой банк, создав запись метаданных, записав в них информацию о затребовавших QR-логах, и сохранив данные вопроса в базу данных
                    HashMap<QuestionMetadataEntity, Integer> matchedMetadata = new HashMap<>();
                    if (generationRequests == null) {
                        for (QuestionMetadataEntity meta : metaList) {
                            matchedMetadata.put(meta, null);
                        }
                    } else {
                        for (QuestionMetadataEntity meta : metaList) {
                            if (matchedMetadata.containsKey(meta))
                                continue; // already matched

                            for (var gr : incompletedRequests.entrySet()) {
                                if (matchedMetadata.containsKey(meta))
                                    break; // already matched
                                if (gr.getValue().isEmpty()) {
                                    continue;
                                }

                                var searchRequest = gr.getKey().getQuestionRequest();
                                var genRequest = gr.getValue().stream().findFirst().orElse(null);
                                if (!storage.isMatch(meta, searchRequest)) {
                                    log.debug("Question [{}] does not match generation requests group {}", q.getCommonQuestion().getQuestionData().getQuestionName(), gr.getKey().getGenerationRequestIds());
                                } else {
                                    matchesRequest.addAndGet(1);
                                    log.debug("Question [{}] matches generation requests group {}", q.getCommonQuestion().getQuestionData().getQuestionName(), gr.getKey().getGenerationRequestIds());
                                    matchedMetadata.put(meta, genRequest.id());
                                    var qGenerated = questionsGenerated.compute(genRequest, (k, v) -> v == null ? 1 : v + 1);

                                    if (qGenerated >= genRequest.questionsToGenerate()) {
                                        gr.getValue().remove(genRequest);
                                    }
                                }
                            }
                        }
                    }

                    if (matchedMetadata.isEmpty()) {
                        skippedQuestions.addAndGet(1);
                        log.debug("Question [{}] skipped because zero qr matches: ", q.getCommonQuestion().getQuestionData().getQuestionName());
                        continue;
                    }

                    if (generatorConfig.isSaveToDb()) {
                        // save question data in the database
                        QuestionDataEntity questionData = new QuestionDataEntity();
                        questionData.setData(q.getCommonQuestion());

                        // then save metadata
                        for (var kv : matchedMetadata.entrySet()) {
                            var meta = kv.getKey();
                            var genRequestId = kv.getValue();
                            meta.setGenerationRequestId(genRequestId);
                            meta.setQuestionData(questionData);

                            metadataToSave.add(meta);
                        }

                        /*
                        log.debug("* * *");
                        log.debug("Question [{}] saved with data in database. Metadata id: {}", q.getCommonQuestion().getQuestionData().getQuestionName(),
                                metaList.stream()
                                        .map(QuestionMetadataEntity::getId)
                                        .map((Integer i) -> i == null ? "ERROR" : Integer.toString(i))
                                        .collect(Collectors.joining(", ")));
                        savedQuestions.addAndGet(1);
                        */
                    } else {
                        log.debug("Saving updates to DB actually SKIPPED due to DEBUG mode:");
                    }
                }
                
                if (!metadataToSave.isEmpty()) {
                    storage.saveMetadataWithDataEntities(metadataToSave);
                    savedQuestions.addAndGet(metadataToSave.size());
                }

                if (matchesRequest.get() == 0 && !incompletedRequests.isEmpty()) {
                    log.info("None of the questions matched to any of incompleted generation requests. Skipping...");
                }
                log.info("Processed {}/{} questions ({} skipped (as existing {}), {} saved).",
                        processed.addAndGet(batch.size()), allJsonFiles.size(),
                        skippedQuestions, existingQuestions, savedQuestions);
            });
        }

        // update QR-log: statistics and possibly status
        if (generationRequests != null) {
            if (generatorConfig.isSaveToDb()) {
                for (var gr : generationRequests) {
                    generatorRequestsQueue.updateGenerationRequests(gr.getGenerationRequestIds());
                }
            } else {
                log.info("Saving updates actually SKIPPED due to DEBUG mode.");
            }
        }
    }

    /**
     * A class to crawl GitHub repositories using the GitHub API.
     * It loads certain number of repositories in the background and allows for cyclic iteration over them.
     * It uses a background thread to load repositories and a scheduled executor to invalidate the cache every 3 hours.
     */
    public static class RepositoriesCrawler implements Iterable<GHRepository>, AutoCloseable {
        private final LinkedHashSet<GHRepository> repositories = new LinkedHashSet<>();
        private final String githubOAuthToken;
        private final int maxRepositories;
        private final String defaultQuery;
        private final Object lock = new Object();
        private volatile boolean loadingFinished = false;
        private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

        public RepositoriesCrawler(String githubOAuthToken, int maxRepositories, String defaultQuery) {
            this.githubOAuthToken = githubOAuthToken;
            this.maxRepositories = maxRepositories;
            this.defaultQuery = defaultQuery;

            // Start background loading
            startBackgroundLoading();

            // Schedule invalidation every 3 hours
            scheduledExecutor.scheduleAtFixedRate(() -> {
                if (!loadingFinished) {
                    log.info("Repositories are still being loaded, skipping invalidation.");
                    return;
                }

                synchronized (lock) {
                    if (!loadingFinished) {
                        log.info("Repositories are still being loaded, skipping invalidation.");
                        return;
                    }

                    repositories.clear();
                    loadingFinished = false;
                }

                log.info("Invalidating cache and starting background loading again...");
                startBackgroundLoading();
            }, 3, 3, TimeUnit.HOURS);
        }

        private void startBackgroundLoading() {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    loadAll();
                } catch (Exception e) {
                    log.error("Error loading repositories: {}", e.getMessage(), e);
                } finally {
                    executor.shutdown();
                }
            });
        }

        @SneakyThrows
        private void loadAll() {
            while (!loadingFinished) {
                try {
                    log.info("Start crawling repositories...");
                    
                    GitHub github = new GitHubBuilder()
                            .withOAuthToken(githubOAuthToken)
                            .withRateLimitChecker(new RateLimitChecker.LiteralValue(20), RateLimitTarget.SEARCH)
                            .build();

                    String query = defaultQuery != null ? defaultQuery :
                            "language:C language:C++ language:Python language:Java";
                    List<PagedSearchIterable<GHRepository>> repoSearchQueries = new ArrayList<>();
                    repoSearchQueries.add(github.searchRepositories()
                            .q(query)
                            .size("50..100000")
                            .fork(GHFork.PARENT_ONLY)
                            .sort(GHRepositorySearchBuilder.Sort.STARS)
                            .order(GHDirection.DESC)
                            .list());
                    repoSearchQueries.add(github.searchRepositories()
                            .language(query)
                            .size("50..100000")
                            .fork(GHFork.PARENT_ONLY)
                            .sort(GHRepositorySearchBuilder.Sort.UPDATED)
                            .order(GHDirection.DESC)
                            .list()
                            .withPageSize(100));
                    repoSearchQueries.add(github.searchRepositories()
                            .language(query)
                            .size("50..100000")
                            .fork(GHFork.PARENT_ONLY)
                            .sort(GHRepositorySearchBuilder.Sort.FORKS)
                            .order(GHDirection.DESC)
                            .list()
                            .withPageSize(100));

                    for (PagedSearchIterable<GHRepository> repoSearchQuery : repoSearchQueries) {
                        for (GHRepository repo : repoSearchQuery) {
                            synchronized (lock) {
                                if (repositories.size() >= maxRepositories) {
                                    log.info("Reached the limit of repositories in the buffer, stopping loading.");
                                    loadingFinished = true;
                                    return;
                                }

                                repositories.add(repo);
                            }
                        }
                        Thread.sleep(2000L);
                        log.debug("Slept {}ms to avoid GitHub API abuse", 2000);
                    }
                } catch (Exception e) {
                    log.error("Error loading repositories: {}", e.getMessage(), e);
                    Thread.sleep(5000L);
                }
                Thread.sleep(1000L);
            }
        }

        /**
         * Returns the next repository in a cyclic manner.
         */
        public GHRepository getNext() {
            synchronized (lock) {
                // If the collection is empty, return null or wait (depending on your requirements)
                if (repositories.isEmpty()) {
                    return null;
                }
                // Create an iterator on the current collection
                Iterator<GHRepository> it = repositories.iterator();
                GHRepository repo = it.next();
                // Remove it from the beginning and add it at the end to cycle through
                it.remove();
                repositories.add(repo);
                return repo;
            }
        }

        /**
         * A cyclic iterator that continuously cycles through repositories.
         * Note: This iterator never ends. You can enhance it by checking whether loading has finished
         * and whether no repository is available if that fits your use case.
         */
        @NotNull
        @Override
        public Iterator<GHRepository> iterator() {
            return new Iterator<GHRepository>() {
                @Override
                public boolean hasNext() {
                    return !repositories.isEmpty() || !loadingFinished;
                }

                @Override
                public GHRepository next() {
                    GHRepository nextRepo = getNext();
                    while (nextRepo == null) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        nextRepo = getNext();
                    }
                    if (nextRepo == null) {
                        throw new NoSuchElementException("No repositories available at this time");
                    }
                    return nextRepo;
                }
            };
        }

        /**
         * Provides a stream interface to iterate over repositories cyclically.
         * This stream is infinite. Use limit() or takeWhile() to avoid infinite iteration.
         */
        public Stream<GHRepository> stream() {
            // Use the Iterable to create a spliterator for use in a stream.
            return StreamSupport.stream(this.spliterator(), false);
        }

        @Override
        public void close() throws Exception {
            try {
                scheduledExecutor.close();
            } catch (Exception ignored) {
            }
        }
    }
}
