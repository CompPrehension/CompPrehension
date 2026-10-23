package org.vstu.compprehension.service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "compprehension.outbox.processing-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {

    private final OutboxProcessor processor;

    @Scheduled(fixedDelayString = "${compprehension.outbox.poll-interval:PT5S}")
    public void processDueEvents() {
        processor.processDueEvents(Instant.now());
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    public void reportStuckEvents() {
        processor.reportStuckEvents(Instant.now());
    }
}
