package org.vstu.compprehension.jobs.tasksgeneration;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.jobrunr.jobs.annotations.Job;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.storage.AbstractRdfStorage;
import org.vstu.compprehension.models.businesslogic.storage.LocalRdfStorage;
import org.vstu.compprehension.models.businesslogic.storage.RemoteFileService;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.models.repository.QuestionRequestLogRepository;
import org.vstu.compprehension.utils.FileUtility;
import org.vstu.compprehension.utils.ZipUtility;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    @Autowired
    public TaskGenerationJob(QuestionRequestLogRepository qrLogRep, QuestionMetadataRepository metadataRep, TaskGenerationJobConfig config) {
        this.qrLogRep = qrLogRep;
        this.metadataRep = metadataRep;
        this.config = config;
    }

    @SneakyThrows
    @Job
    public void run() {
        log.info("Run generating questions for expression domain");
        try {
            runGeneration4Expr();
        } catch (Exception e) {
            log.warn("job exception", e);
        }
        System.exit(0);
    }

    /**
     * @return true if more processing needed
     */
    @SneakyThrows
    public boolean runGeneration4Expr() {

        /*
        // TODO проверка на то, что нужны новые вопросы
        int enoughQuestionsAdded = AbstractRdfStorage.getQrEnoughQuestions(0);  // mark QRLog resolved if such many questions were added (e.g. 150)
        */
        int tooFewQuestions = AbstractRdfStorage.getTooFewQuestionsForQR(0); // (e.g. 50)
        var qrLogsToProcess = qrLogRep.findAllNotProcessed(config.getDomainShortName(), tooFewQuestions);

        if (qrLogsToProcess.isEmpty()) {
            log.info("Nothing to process, finished job.");
            return false;
        }

        boolean _debugGenerator = false;
        boolean cleanupFolders = false; // !_debugGenerator;
        boolean cleanupGeneratedFolder = false;
        boolean downloadRepositories = !_debugGenerator;
        boolean parseSources = !_debugGenerator;
        boolean generateQuestions = !_debugGenerator;
        boolean exportQuestionsToProduction = true;
        
        int repositoriesToDownload = 1;


        // folders cleanup
        if (cleanupFolders) {
            if (Files.exists(Path.of(config.getSearcher().getOutputFolderPath())))
                FileUtils.deleteDirectory(new File(config.getSearcher().getOutputFolderPath()));
            if (Files.exists(Path.of(config.getParser().getOutputFolderPath())))
                FileUtils.deleteDirectory(new File(config.getParser().getOutputFolderPath()));

            // don't delete output: it may be reused later
            if (cleanupGeneratedFolder) {
                if (Files.exists(Path.of(config.getGenerator().getOutputFolderPath())))
                    FileUtils.deleteDirectory(new File(config.getGenerator().getOutputFolderPath()));
            }

            log.info("folders cleaned up");
        }

        List<Path> downloadedRepos;

        // download repos (currently, one repository per run)
        if (downloadRepositories) {
            // TODO Добавить историю по использованным репозиториям
            var seenReposNames = metadataRep.findAllOrigins(config.getDomainShortName() /*, 3 == STAGE_QUESTION_DATA*/);

            GitHub github = new GitHubBuilder()
                    .withOAuthToken(config.getSearcher().getGithubOAuthToken())
                    .build();
            var repoSearchQuery = github.searchRepositories()
                    .language("c")
                    .size("50..100000")
                    .fork(GHFork.PARENT_ONLY)
                    .sort(GHRepositorySearchBuilder.Sort.STARS)
                    .order(GHDirection.DESC)
                    .list()
                    .withPageSize(30);
            downloadedRepos = new ArrayList<Path>();
            {
                int idx = 0;
                for (var repo : repoSearchQuery) {
                    if (seenReposNames.contains(repo.getName())) {
                        log.info("Skip processed GitHub repo: " + repo.getName());
                        continue;
                    }
                    log.info("Downloading repo [{}] ...", repo.getFullName());

                    repo.readZip(s -> {
                        Path zipFile = Path.of(config.getSearcher().getOutputFolderPath(), repo.getName() + ".zip")
                                .toAbsolutePath();
                        Path targetFolderPath = Path.of(config.getSearcher().getOutputFolderPath(), repo.getName())
                                .toAbsolutePath();
                        Files.createDirectories(targetFolderPath);
                        java.nio.file.Files.copy(s, zipFile, StandardCopyOption.REPLACE_EXISTING);
                        ZipUtility.unzip(zipFile.toString(), targetFolderPath.toString());
                        Files.delete(zipFile);
                        downloadedRepos.add(targetFolderPath);
                        log.info("Downloaded repo [{}] to location [{}]", repo.getFullName(), targetFolderPath);
                        return 0;
                    }, null);

                     // for now, limited number of repositories
                     if (++idx >= repositoriesToDownload)
                        break;
                }
            }
            // end of download
        } else {
            // get all downloaded so far:
            var rootDir = Path.of(config.getSearcher().getOutputFolderPath());
            downloadedRepos = Files.list(rootDir).filter(Files::isDirectory).collect(Collectors.toList());
        }

        // do parsing
        if (parseSources)
        {
            for(var repo : downloadedRepos) {
                log.info("Start parsing sources for repo [{}]", repo);

                String leafFolder = repo.getFileName().toString();
                Path destination = Path.of(config.getParser().getOutputFolderPath(), leafFolder);

                var files = FileUtility.findFiles(repo, new String[] { ".c", ".m" });
                files = files.subList(0, Math.min(50, files.size()));
                log.info("Found {} *.c & *.m files", files.size());

                List<String> parserProcessCommandBuilder = new ArrayList<>();
                parserProcessCommandBuilder.add(config.getParser().getPathToExecutable());
                parserProcessCommandBuilder.addAll(files);

                // reduce number of arguments on command line if necessary
                // TODO: loop over batches if required to split the task
                parserProcessCommandBuilder = FileUtility.truncateLongCommandline(parserProcessCommandBuilder, 2+10+5 + destination.toString().length());

                parserProcessCommandBuilder.add("--");
                parserProcessCommandBuilder.add(config.getDomainShortName());  // e.g. "expression"
                parserProcessCommandBuilder.add(destination.toString());
                log.debug("Parser executable command: {}", parserProcessCommandBuilder);

                try {
                    log.info("Run parser on repo: [{}]", leafFolder);
                    Files.createDirectories(destination);
                    var parserProcess = new ProcessBuilder(parserProcessCommandBuilder)
                            .redirectErrorStream(true)
                            .start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(parserProcess.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) { // do not remove this cycle! waitFor wouldn't work without it
                        log.debug(line);
                    }
                    parserProcess.waitFor(10, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    log.warn("Parsing timeout exception", e);
                } catch (Exception e) {
                    log.warn("Parsing exception", e);
                }

                log.info("Repo [{}] parsed", repo);
            }
        }

        // question generation step
        if (generateQuestions) {
            // find all sub-folders in root directory of parser output directory
            var rootDir = Path.of(config.getParser().getOutputFolderPath());
            var repos = Files.list(rootDir).filter(Files::isDirectory).collect(Collectors.toList());

            log.info("Start question generation from " + repos.size() + " repository(-ies) ...");

            for (var repoDir : repos) {
                var allTtlFiles = FileUtility.findFiles(repoDir, new String[]{".ttl"});
                log.info("Found {} ttl files in: {}", allTtlFiles.size(), repoDir);

                String leafFolder = repoDir.getFileName().toString();
                String questionOrigin = leafFolder;
                Path destination = Path.of(config.getGenerator().getOutputFolderPath(), questionOrigin);
                Files.createDirectories(destination);

                List<String> cmd = new ArrayList<>();

                cmd.add(config.getGenerator().getPathToExecutable());
                cmd.add("--source");
                cmd.add(String.valueOf(repoDir));
                cmd.add("--output");
                cmd.add(String.valueOf(destination));
                cmd.add("--sourceId");
                cmd.add(leafFolder);


                // TODO: call specific generation tool

                try {
                    log.info("Run generator on repo: [{}]", leafFolder);
                    Files.createDirectories(destination);
                    var parserProcess = new ProcessBuilder(cmd)
                            .redirectErrorStream(true)
                            .start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(parserProcess.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) { // do not remove this cycle! waitFor wouldn't work without it
                        log.debug(line);
                    }
                    parserProcess.waitFor(10, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    log.warn("Question generation timeout exception", e);
                } catch (Exception e) {
                    log.warn("Question generation exception", e);
                }

                log.info("Repo [{}] used to make questions.", leafFolder);


                // загрузить из папки полученные вопросы

                var allJsonFiles = FileUtility.findFiles(repoDir, new String[]{".json"});
                log.info("Found {} json files in: {}", allJsonFiles.size(), repoDir);

                LocalRdfStorage storage = getQuestionStorage();

                for (val file : allJsonFiles) {
                    val q = loadQuestion(file);
                    if (q == null) {
                        continue;
                    }

                    QuestionMetadataEntity meta = q.getMetadataAny();
                    if (meta == null) {
                        // в вопросе нет метаданных, Невозможно проверить
                        log.warn("[info] cannot save question which does not contain metadata. Source file: " + file);
                        continue;
                    }

                    // init container: list of QR logs requiring this question
                    meta.setQrlogIds(new ArrayList<>());

                    // Проверить, подходит ли он нам
                    // если да, то сразу импортировать его в боевой банк, создав запись метаданных, записав в них информацию о затребовавших QR-логах, и скопировав файл с данными вопроса...
                    boolean shouldSave = false;
                    for (val qr : qrLogsToProcess) {
                        if (QuestionRequestLogRepository.doesQuestionSuitQR(meta, qr)) {
                            // TODO
                            shouldSave = true;
                            meta.getQrlogIds().add(qr.getId());
                        }
                    }

                    if (shouldSave) {

                        // copy data file and save local sub-path to it
                        String qDataPath = storage.saveQuestionData(q.getQuestionName(), Domain.questionToJson(q, "ORDERING"));
                        meta.setQDataGraph(qDataPath);

                        log.info("(1) Saved data file for question: [{}] ([{}])", q.getQuestionName(), qDataPath);

                        // set more metadata
                        meta.setDateCreated(new Date());
                        meta.setUsedCount(0L);
                        meta.setOrigin(questionOrigin);
                        // meta.setDateLastUsed(null);
                        // meta.setDomainShortname(config.getDomainShortName());

                        // set metadata instance back to ensure the data is saved
                        q.setMetadata(meta);
                        q.getQuestionData().getOptions().setMetadata(meta);

                        meta = storage.saveMetadataEntity(meta);
                        log.info("(2) Saved metadata for that question, id: [{}]", meta.getId());
                    }
                }


                // for Expression domain only:
                //RdfStorage.generateQuestionsForExpressionsDomain(repoDir.toString(), config.getGenerator().getOutputFolderPath(), config.getExporter().getStorageDummyDirsForNewFile(), leafFolder);
            }
        }

        // export generated questions to production bank if necessary
        /*
        if (exportQuestionsToProduction) {
            log.info("Start exporting questions to production bank ...");
            AbstractRdfStorage.exportGeneratedQuestionsToProductionBank(
                    qrLogsToProcess, enoughQuestionsAdded,
                    metadataRep, qrLogRep,
                    config.getGenerator().getOutputFolderPath(), config.getExporter().getStorageUploadFilesBaseUrl(), config.getExporter().getStorageDummyDirsForNewFile());
        }
        */

        log.info("completed");

        return true;
    }

    /**
     * @param inputStream file contents
     * @return loaded question
     * @see Domain::parseQuestionTemplate
     */
    private static Question parseQuestionJson(InputStream inputStream) {
        Gson gson = Domain.getQuestionGson();

        Question question = gson.fromJson(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                Question.class);

        return question;
    }

    /**
     * @param path absolute path to file location
     * @return loaded question
     * @see org.vstu.compprehension.models.businesslogic.storage.AbstractRdfStorage::loadQuestion
     */
    private static Question loadQuestion(String path) {
        Question q = null;
        try (InputStream stream = new FileInputStream(path)) {
            q = parseQuestionJson(stream);
        } catch (IOException | NullPointerException | IllegalStateException e) {
            e.printStackTrace();
        }
        return q;
    }


    private LocalRdfStorage getQuestionStorage() {
        return new LocalRdfStorage(
                new RemoteFileService(
                        config.getExporter().getStorageUploadFilesBaseUrl(),
                        config.getExporter().getStorageUploadFilesBaseUrl(),
                        config.getExporter().getStorageDummyDirsForNewFile()),
                metadataRep,
                null);
    }
}
