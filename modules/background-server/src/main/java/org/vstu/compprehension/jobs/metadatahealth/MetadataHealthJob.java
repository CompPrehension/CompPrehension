package org.vstu.compprehension.jobs.metadatahealth;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.repository.data.QuestionBankDataRepository;

@Log4j2
@Service
public class MetadataHealthJob {
    private final QuestionBankDataRepository bank;
    private final MetadataHealthJobConfig config;

    public MetadataHealthJob(QuestionBankDataRepository bank, MetadataHealthJobConfig config) {
        this.bank   = bank;
        this.config = config;
    }

    @Job(name = "metadata-health-job", retries = 0)
    public void run() {
        try {
            runImpl();
        } catch (Exception e) {
            log.error("Metadata health job exception - {}", e.getMessage(), e);
            throw e;
        }
    }

    @SneakyThrows
    private synchronized void runImpl() {
        removeTooOldGenerationRequestStats();
    }

    private void removeTooOldGenerationRequestStats() {
        log.info("Starting to remove too old generation requests");
        var nonProcessableRequests = bank.findStuckGenerationRequestIds(5000);
        if (nonProcessableRequests.isEmpty()) {
            log.info("No too old generation requests found");
            return;
        }

        log.info("Found {} non processable generation requests: {}", nonProcessableRequests.size(), nonProcessableRequests);
        bank.cancelGenerationRequests(nonProcessableRequests);
        log.info("Cancelled {} non processable generation requests", nonProcessableRequests.size());

        log.info("Finished removing too old generation requests");
    }
}
