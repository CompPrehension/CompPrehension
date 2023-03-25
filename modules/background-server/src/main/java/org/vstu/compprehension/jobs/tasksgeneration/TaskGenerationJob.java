package org.vstu.compprehension.jobs.tasksgeneration;

import com.google.common.collect.Iterators;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jobrunr.jobs.annotations.Job;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.repository.ExpressionQuestionMetadataRepository;
import org.vstu.compprehension.utils.FileUtility;
import org.vstu.compprehension.utils.ZipUtility;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class TaskGenerationJob {
    private final ExpressionQuestionMetadataRepository matadataRep;
    private final TaskGenerationJobConfig config;

    @Autowired
    public TaskGenerationJob(ExpressionQuestionMetadataRepository matadataRep, TaskGenerationJobConfig config) {
        this.matadataRep = matadataRep;
        this.config = config;
    }

    @SneakyThrows
    @Job
    public void run() {
        // TODO проверка на то, что нужны новые вопросы

        // folders cleanup
        if (Files.exists(Path.of(config.getSearcher().getOutputFolderPath())))
            FileUtils.deleteDirectory(new File(config.getSearcher().getOutputFolderPath()));
        if (Files.exists(Path.of(config.getParser().getOutputFolderPath())))
            FileUtils.deleteDirectory(new File(config.getParser().getOutputFolderPath()));
        if (Files.exists(Path.of(config.getGenerator().getOutputFolderPath())))
            FileUtils.deleteDirectory(new File(config.getGenerator().getOutputFolderPath()));
        log.info("folders cleaned up");

        // download repos
        // TODO Добавить историю
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
        var downloadedRepos = new ArrayList<Path>();
        {
            int idx = 0;
            for (var repo : repoSearchQuery) {
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

                // пока только 10 репозиториев
                if (++idx >= 10)
                    break;
            }
        }

        // do parsing
        for(var repo : downloadedRepos) {
            log.info("Start parsing sources for repo {}", repo);

            var files = FileUtility.findFiles(repo, new String[] { "c", "m" });
            log.info("Found {} *.c & *.m files", files.size());

            var parserProcessCommandBuilder = new StringBuilder();
            parserProcessCommandBuilder.append(config.getParser().getPathToExecutable());
            parserProcessCommandBuilder.append(' ');
            for (var file : files) {
                parserProcessCommandBuilder.append(file);
                parserProcessCommandBuilder.append(' ');
            }
            parserProcessCommandBuilder.append("-- ");
            parserProcessCommandBuilder.append(config.getParser().getOutputFolderPath());
            log.info("Parser executable command: {}", parserProcessCommandBuilder);

            try {
                Files.createDirectories(Path.of(config.getParser().getOutputFolderPath()));
                var parserProcess = Runtime.getRuntime().exec(parserProcessCommandBuilder.toString());
                parserProcess.waitFor(10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.warn("Parsing timeout exception", e);
            } catch (Exception e) {
                log.warn("Parsing exception", e);
            }

            log.info("Repo processed");
        }

        log.info("completed");
    }
}
