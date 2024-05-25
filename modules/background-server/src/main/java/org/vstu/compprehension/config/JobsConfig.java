package org.vstu.compprehension.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.context.annotation.Configuration;
import org.vstu.compprehension.jobs.metadatahealth.MetadataHealthJob;
import org.vstu.compprehension.jobs.metadatahealth.MetadataHealthJobConfig;
import org.vstu.compprehension.jobs.tasksgeneration.TaskGenerationJob;
import org.vstu.compprehension.jobs.tasksgeneration.TaskGenerationJobConfig;

@Log4j2
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
        jobScheduler.delete("TaskGenerationJob");
        if (taskGenerationJobConfig.isRunOnce()) {
            jobScheduler.enqueue(TaskGenerationJob::run);
            log.info("TaskGenerationJob scheduled once");
        } else {
            if (!"never".equalsIgnoreCase(taskGenerationJobConfig.getCronSchedule())) {
                jobScheduler.scheduleRecurrently("TaskGenerationJob", taskGenerationJobConfig.getCronSchedule(), TaskGenerationJob::run);
            }
            log.info("TaskGenerationJob scheduled with schedule: {}", taskGenerationJobConfig.getCronSchedule());
        }        

        // MetadataHealthJob
        jobScheduler.delete("MetadataHealthJob");
        if (metadataHealthJobConfig.isRunOnce()) {
            jobScheduler.enqueue(MetadataHealthJob::run);
            log.info("MetadataHealthJob scheduled once");
        } else {
            if (!"never".equalsIgnoreCase(metadataHealthJobConfig.getCronSchedule())) {
                jobScheduler.scheduleRecurrently("MetadataHealthJob", metadataHealthJobConfig.getCronSchedule(), MetadataHealthJob::run);
            }
            log.info("MetadataHealthJob scheduled with schedule: {}", metadataHealthJobConfig.getCronSchedule());
        }
    }
}
