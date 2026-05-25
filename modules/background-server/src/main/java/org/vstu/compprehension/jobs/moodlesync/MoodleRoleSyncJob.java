package org.vstu.compprehension.jobs.moodlesync;

import lombok.extern.log4j.Log4j2;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class MoodleRoleSyncJob {
    private final MoodleRoleSyncService syncService;

    public MoodleRoleSyncJob(MoodleRoleSyncService syncService) {
        this.syncService = syncService;
    }

    @Job(name = "moodle-role-sync-job", retries = 0)
    public void run() {
        try {
            syncService.syncAll();
        } catch (Exception e) {
            log.error("Moodle role sync job failure - {}", e.getMessage(), e);
            throw e;
        }
    }
}
