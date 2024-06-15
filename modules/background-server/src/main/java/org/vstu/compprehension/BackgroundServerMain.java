package org.vstu.compprehension;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.JobActivator;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@SpringBootApplication
public class BackgroundServerMain {
    public static void main(String[] args) {
        SpringApplication.run(BackgroundServerMain.class, args);
    }

    @Bean
    public JobScheduler initJobRunr(JobActivator jobActivator, StorageProvider storageProvider) {
        return JobRunr.configure()
                .useJobActivator(jobActivator)
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer()
                .useDashboard()
                .initialize().getJobScheduler();
    }

    @Bean
    public StorageProvider storageProvider(JobMapper jobMapper, DataSource dataSource) {
        var storageProvider = SqlStorageProviderFactory.using(dataSource, "jobs_");
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }
}
