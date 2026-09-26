package org.vstu.compprehension.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Обработка очереди событий в БД.
 */
@Component
@ConfigurationProperties(prefix = "compprehension.outbox")
@Getter
@Setter
public class OutboxProperties {
    /** Выключенная обработка не останавливает публикацию: события копятся и ждут включения. */
    private boolean processingEnabled = true;

    private int batchSize = 20;

    /** После стольких неудачных попыток событие становится FAILED и больше не обрабатывается. */
    private int maxAttempts = 10;

    /** Обработанные события остаются со статусом PROCESSED (история для экспериментов), иначе удаляются. */
    private boolean keepProcessedEvents = true;
}
