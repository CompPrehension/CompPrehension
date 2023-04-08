package org.vstu.compprehension;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.JobActivator;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.vstu.compprehension.jobs.tasksgeneration.TaskGenerationJob;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;

@SpringBootApplication
public class BackgroundServerMain {
    @Autowired
    private JobScheduler jobScheduler;

    public static void main(String[] args) throws IOException {
        SpringApplication.run(BackgroundServerMain.class, args);
    }

    @Bean
    public JobScheduler initJobRunr(JobActivator jobActivator) {
        return JobRunr.configure()
                .useJobActivator(jobActivator)
                .useStorageProvider(new InMemoryStorageProvider())
                .useBackgroundJobServer()
                .useDashboard()
                .initialize()
                .getJobScheduler();
    }

    @PostConstruct
    public void jobsConfig() {
//        jobScheduler.<TaskGenerationJob>enqueue(TaskGenerationJob::run);
        jobScheduler.<TaskGenerationJob>scheduleRecurrently(Duration.ofHours(2), TaskGenerationJob::run);
        /* Note:
        Jobrunr does not rub a recurrent job if the previous run hasn't finished yet. (That's OK for us.)
        * */
    }
}