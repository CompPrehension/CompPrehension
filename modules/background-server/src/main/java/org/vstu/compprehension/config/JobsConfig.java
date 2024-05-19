package org.vstu.compprehension.config;

import jakarta.annotation.PostConstruct;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.context.annotation.Configuration;
import org.vstu.compprehension.jobs.tasksgeneration.TaskGenerationJob;
import org.vstu.compprehension.jobs.metadatahealth.MetadataHealthJob;
import org.vstu.compprehension.jobs.metadatahealth.MetadataHealthJobConfig;
import org.vstu.compprehension.jobs.tasksgeneration.TaskGenerationJobConfig;

@Configuration
public class JobsConfig {
    private final JobScheduler jobScheduler;
    private final TaskGenerationJobConfig taskGenerationJobConfig;
    private final MetadataHealthJobConfig metadataHealthJobConfig;

    public JobsConfig(JobScheduler jobScheduler, TaskGenerationJobConfig taskGenerationJobConfig, MetadataHealthJobConfig metadataHealthJobConfig) {
        this.jobScheduler            = jobScheduler;
        this.taskGenerationJobConfig = taskGenerationJobConfig;
        this.metadataHealthJobConfig = metadataHealthJobConfig;
    }

    @PostConstruct
    public void jobsConfig() {
        // TaskGenerationJob
        if (taskGenerationJobConfig.isRunOnce()) {
            jobScheduler.enqueue(TaskGenerationJob::run);
        } else {
            jobScheduler.scheduleRecurrently("TaskGenerationJob", taskGenerationJobConfig.getCronSchedule(), TaskGenerationJob::run);
        }        

        // MetadataHealthJob
        if (metadataHealthJobConfig.isRunOnce()) {
            jobScheduler.enqueue(MetadataHealthJob::run);
        } else {
            jobScheduler.scheduleRecurrently("MetadataHealthJob", metadataHealthJobConfig.getCronSchedule(), MetadataHealthJob::run);
        }
    }
}
