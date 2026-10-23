package org.vstu.compprehension.jobs.ltimembership;

import lombok.extern.log4j.Log4j2;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.services.CourseMembershipSyncService;

@Log4j2
@Service
public class LtiMembershipSyncJob {
    private final CourseMembershipSyncService syncService;

    public LtiMembershipSyncJob(CourseMembershipSyncService syncService) {
        this.syncService = syncService;
    }

    @Job(name = "lti-membership-sync-job", retries = 0)
    public void run() {
        log.info("LTI membership sync started");
        long startedAt = System.currentTimeMillis();

        syncService.syncAll();

        log.info("LTI membership sync finished in {} ms", System.currentTimeMillis() - startedAt);
    }
}
