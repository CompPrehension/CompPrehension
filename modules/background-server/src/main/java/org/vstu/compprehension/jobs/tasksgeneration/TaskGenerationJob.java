package org.vstu.compprehension.jobs.tasksgeneration;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.jobrunr.jobs.annotations.Job;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.businesslogic.storage.AbstractRdfStorage;
import org.vstu.compprehension.models.businesslogic.storage.RdfStorage;
import org.vstu.compprehension.models.repository.QuestionMetadataBaseRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataDraftRepository;
import org.vstu.compprehension.models.repository.QuestionRequestLogRepository;
import org.vstu.compprehension.utils.FileUtility;
import org.vstu.compprehension.utils.ZipUtility;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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
    private final QuestionMetadataBaseRepository metadataRep;
    private final QuestionMetadataDraftRepository metadataDraftRep;
    private final TaskGenerationJobConfig config;

    @Autowired
    public TaskGenerationJob(QuestionRequestLogRepository qrLogRep, QuestionMetadataBaseRepository metadataRep, QuestionMetadataDraftRepository metadataDraftRep, TaskGenerationJobConfig config) {
        this.qrLogRep = qrLogRep;
        this.metadataRep = metadataRep;
        this.metadataDraftRep = metadataDraftRep;
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

        // boolean _debugGenerator = false;
        boolean cleanupFolders = false;
        boolean cleanupGeneratedFolder = false;
        boolean downloadRepositories = false;
        boolean parseSources = true;
        boolean generateQuestions = true;
        boolean exportQuestionsToProduction = true;
        
        int repositoriesToDownload = 10;


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
        if (downloadRepositories)
        {
            // TODO Добавить историю
            var seenReposNames = metadataDraftRep.findAllOrigins(config.getDomainShortName() /*, 3 == STAGE_QUESTION_DATA*/);

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
                parserProcessCommandBuilder.add("--");
                parserProcessCommandBuilder.add("expression");
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
                        //log.info("[Parser output] - {}", line);
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

                // for Expression domain only:
                RdfStorage.generateQuestionsForExpressionsDomain(repoDir.toString(), config.getGenerator().getOutputFolderPath(), config.getExporter().getStorageDummyDirsForNewFile(), leafFolder);
            }
        }

        // export generated questions to production bank if necessary
        if (exportQuestionsToProduction) {
            log.info("Start exporting questions to production bank ...");
            AbstractRdfStorage.exportGeneratedQuestionsToProductionBank(
                    // qrLogsToProcess, enoughQuestionsAdded,
                    config.getDomainShortName(),
                    metadataRep, metadataDraftRep,
                    //qrLogRep,
                    config.getGenerator().getOutputFolderPath(), config.getExporter().getStorageUploadFilesBaseUrl(), config.getExporter().getStorageDummyDirsForNewFile());
        }

        log.info("completed");

        return true;
    }
}
