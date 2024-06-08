package org.vstu.compprehension.jobs.metadatahealth;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.DomainRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Log4j2
@Service
public class MetadataHealthJob {
    private final QuestionMetadataRepository metadataRep;
    private final DomainRepository domainRep;
    private final MetadataHealthJobConfig config;

    public MetadataHealthJob(QuestionMetadataRepository metadataRep, DomainRepository domainRep, MetadataHealthJobConfig config) {
        this.metadataRep = metadataRep;
        this.domainRep   = domainRep;
        this.config      = config;
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
    public synchronized void runImpl() {
        log.info("Starting metadata health job. Mode: {}", config.getMode());
        
        var domains = domainRep.findAll()
                .stream()
                .collect(Collectors.toMap(DomainEntity::getShortName, domain -> domain));
        
        int pageSize     = 5_000;        
        long total       = metadataRep.count();
        int processed    = 0;
        int deleted      = 0;
        int lastLoadedId = 0;
        while (true) {
            var metadataChunk = metadataRep.loadPage(lastLoadedId, pageSize);
            if (metadataChunk.isEmpty()) {
                break;
            }
            lastLoadedId = metadataChunk.getLast().getId();

            var toDelete = new ArrayList<QuestionMetadataEntity>();
            for (var metadata : metadataChunk) {
                var domain = domains.get(metadata.getDomainShortname());
                if (domain == null) {
                    log.error("Domain not found: {} for metadata with id: {}", metadata.getDomainShortname(), metadata.getId());
                    continue;
                }
                
                var fullMetadataPath = Path.of(Paths.get(new URI(domain.getOptions().getStorageDownloadFilesBaseUrl())).toAbsolutePath().toString(), metadata.getQDataGraph());
                if (fullMetadataPath.toFile().exists()) {
                    continue;
                }
                
                log.warn("Question file not found for metadata {} with path: {}", metadata.getId(), fullMetadataPath);
                toDelete.add(metadata);
            }

            if (!toDelete.isEmpty()) {
                if (config.getMode() == MetadataHealthJobConfig.Mode.DELETE_INVALID) {
                    metadataRep.deleteAll(toDelete);
                    deleted += toDelete.size();
                    log.info("Deleted {} metadata entities", toDelete.size());
                }
            }

            processed += metadataChunk.size();
            log.printf(Level.INFO, "Processed %d/%d (%2.2f%%) metadata entities", processed, total, 100f * processed / total);
        }
        
        log.info("Finished metadata health job. Deleted {} metadata entities", deleted);
    }
}
