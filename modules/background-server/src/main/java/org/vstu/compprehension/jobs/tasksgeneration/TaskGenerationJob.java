package org.vstu.compprehension.jobs.tasksgeneration;

import com.google.common.collect.Iterators;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jobrunr.jobs.annotations.Job;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.repository.ExpressionQuestionMetadataRepository;
import org.vstu.compprehension.utils.ZipUtility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

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
        if (Files.exists(Path.of(config.getPathToRepos())))
            FileUtils.deleteDirectory(new File(config.getPathToRepos()));
        if (Files.exists(Path.of(config.getParser().getOutputFolderPath())))
            FileUtils.deleteDirectory(new File(config.getParser().getOutputFolderPath()));
        if (Files.exists(Path.of(config.getGenerator().getOutputFolderPath())))
            FileUtils.deleteDirectory(new File(config.getGenerator().getOutputFolderPath()));
        log.info("folders cleanupped");

        // download repos
        // TODO Добавить историю
        GitHub github = new GitHubBuilder()
                .withOAuthToken(config.getGithubOAuthToken())
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
                    Path zipFile = Path.of(config.getPathToRepos(), repo.getName() + ".zip")
                            .toAbsolutePath();
                    Path targetFolderPath = Path.of(config.getPathToRepos(), repo.getName())
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
    }
}
