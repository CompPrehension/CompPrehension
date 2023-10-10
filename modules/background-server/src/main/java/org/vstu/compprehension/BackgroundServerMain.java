package org.vstu.compprehension;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.configuration.JobRunrConfiguration;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.vstu.compprehension.jobs.tasksgeneration.TaskGenerationJob;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;

@SpringBootApplication
public class BackgroundServerMain {
    @Autowired
    private JobScheduler jobScheduler;

    /** Whether to execute the job once and then exit; otherwise the job will be called permanently with the specified INTERVAL */
    @Value("${task-generation.run_once}")
    private boolean runOnce;
    @Value("${task-generation.interval_minutes}")
    private int intervalMinutes;


    public static void main(String[] args) throws IOException {
        SpringApplication.run(BackgroundServerMain.class, args);
    }

    @Bean
    public JobRunrConfiguration.JobRunrConfigurationResult initJobRunr(ApplicationContext applicationContext) {
        return JobRunr.configure()
                .useJobActivator(applicationContext::getBean)
                .useStorageProvider(new InMemoryStorageProvider())
                .useBackgroundJobServer()
                .useDashboard()
                .initialize();
    }

    @Bean
    public JobScheduler initJobScheduler(JobRunrConfiguration.JobRunrConfigurationResult jobRunrConfigurationResult) {
        return jobRunrConfigurationResult.getJobScheduler();
    }

    @Bean
    public JobRequestScheduler initJobRequestScheduler(JobRunrConfiguration.JobRunrConfigurationResult jobRunrConfigurationResult) {
        return jobRunrConfigurationResult.getJobRequestScheduler();
    }

    @PostConstruct
    public void jobsConfig() {
        if (runOnce) {
            jobScheduler.<TaskGenerationJob>enqueue(TaskGenerationJob::run);
        } else {
            Duration interval = Duration.ofMinutes(intervalMinutes);
            jobScheduler.<TaskGenerationJob>scheduleRecurrently(interval, TaskGenerationJob::run);
            /* Note:
            Jobrunr does not run a recurrent job if the previous run hasn't finished yet. (That's OK for us.)
            * */
        }
    }
}