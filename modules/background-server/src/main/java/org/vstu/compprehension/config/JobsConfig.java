package org.vstu.compprehension.config;

import jakarta.annotation.PostConstruct;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.vstu.compprehension.jobs.tasksgeneration.TaskGenerationJob;

import java.time.Duration;

@Configuration
public class JobsConfig {
    private final JobScheduler jobScheduler;
    private final boolean runOnce;
    private final int intervalMinutes;

    public JobsConfig(JobScheduler jobScheduler, @Value("${task-generation.run_once}") boolean runOnce, @Value("${task-generation.interval_minutes}") int intervalMinutes) {
        this.jobScheduler    = jobScheduler;
        this.runOnce         = runOnce;
        this.intervalMinutes = intervalMinutes;
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
