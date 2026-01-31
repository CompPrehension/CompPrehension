package org.vstu.compprehension.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.context.annotation.Configuration;
import org.vstu.compprehension.jobs.bankloadtesting.BankLoadTestingJob;
import org.vstu.compprehension.jobs.bankloadtesting.BankLoadTestingJobBatchConfig;
import org.vstu.compprehension.jobs.bankloadtesting.BankLoadTestingJobConfig;
import org.vstu.compprehension.jobs.bankloadtesting.FastBankLoadTestingJob;
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
    private final BankLoadTestingJobConfig bankLoadTestingJobConfig;
    private final BankLoadTestingJobBatchConfig bankLoadTestingJobBatchConfig;

    public JobsConfig(JobScheduler jobScheduler, TaskGenerationJobConfig taskGenerationJobConfig, MetadataHealthJobConfig metadataHealthJobConfig, BankLoadTestingJobConfig bankLoadTestingJobConfig, BankLoadTestingJobBatchConfig bankLoadTestingJobBatchConfig) {
        this.jobScheduler            = jobScheduler;
        this.taskGenerationJobConfig = taskGenerationJobConfig;
        this.metadataHealthJobConfig = metadataHealthJobConfig;
        this.bankLoadTestingJobConfig = bankLoadTestingJobConfig;
        this.bankLoadTestingJobBatchConfig = bankLoadTestingJobBatchConfig;
    }

    @PostConstruct
    public void jobsConfig() {
        // TaskGenerationJob
        {
            var jobId = "TaskGenerationJob";
            var schedule = taskGenerationJobConfig.getCronSchedule();
            scheduleJob(jobId, schedule, TaskGenerationJob::run);
        }

        // MetadataHealthJob
        {
            var jobId = "MetadataHealthJob";
            var schedule = metadataHealthJobConfig.getCronSchedule();
            scheduleJob(jobId, schedule, MetadataHealthJob::run);
        }

        // BankLoadTestingJob
        {
            var jobId = "BankLoadTestingJob";
            var schedule = bankLoadTestingJobConfig.getCronSchedule();
            scheduleJob(jobId, schedule, FastBankLoadTestingJob::run);
        }

        // BankLoadTestingBatchJob
        {
            var jobId = "BankLoadTestingBatchJob";
            var schedule = bankLoadTestingJobBatchConfig.getCronSchedule();
            scheduleJob(jobId, schedule, FastBankLoadTestingJob::runBatch);
        }
    }
    
    private <S> void scheduleJob(String jobId, String schedule, org.jobrunr.jobs.lambdas.IocJobLambda<S> iocJob) {
        jobScheduler.delete(jobId);
        
        if (schedule == null) {
            schedule = "never";
        }
        
        if ("once".equalsIgnoreCase(schedule)) {
            jobScheduler.enqueue(iocJob);
            log.info("{} scheduled once", jobId);
        } else if (!"never".equalsIgnoreCase(schedule)) {
            jobScheduler.scheduleRecurrently(jobId, schedule, iocJob);
            log.info("{} scheduled with schedule: {}", jobId, schedule);
        }
    }
}
