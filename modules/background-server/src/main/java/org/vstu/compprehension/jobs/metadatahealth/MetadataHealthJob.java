package org.vstu.compprehension.jobs.metadatahealth;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.entities.QuestionGenerationRequestEntity;
import org.vstu.compprehension.models.repository.DomainRepository;
import org.vstu.compprehension.models.repository.QuestionGenerationRequestRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

@Log4j2
@Service
public class MetadataHealthJob {
    private final QuestionMetadataRepository metadataRep;
    private final QuestionGenerationRequestRepository genRequestsRep;
    private final DomainRepository domainRep;
    private final MetadataHealthJobConfig config;

    public MetadataHealthJob(QuestionMetadataRepository metadataRep, QuestionGenerationRequestRepository genRequestsRep, DomainRepository domainRep, MetadataHealthJobConfig config) {
        this.metadataRep    = metadataRep;
        this.genRequestsRep = genRequestsRep;
        this.domainRep      = domainRep;
        this.config         = config;
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
        var nonProcessableRequests = genRequestsRep.findAllIdsByStatusAndProcessingAttemptsGreaterThan(QuestionGenerationRequestEntity.Status.ACTUAL, 5000);
        if (nonProcessableRequests.isEmpty()) {
            log.info("No too old generation requests found");
            return;
        }
        
        log.info("Found {} non processable generation requests: {}", nonProcessableRequests.size(), nonProcessableRequests);        
        genRequestsRep.setCancelled(nonProcessableRequests);
        log.info("Cancelled {} non processable generation requests", nonProcessableRequests.size());

        log.info("Finished removing too old generation requests");
    }
}
