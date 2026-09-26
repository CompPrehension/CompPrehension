package org.vstu.compprehension.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vstu.compprehension.enums.OutboxEventStatus;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "outbox_event")
public class OutboxEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Имя константы {@code OutboxEventType}; строка, чтобы процесс не падал на типах, которых он не знает. */
    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "json", updatable = false)
    private String payload;

    @Column(name = "ordering_key", updatable = false)
    private String orderingKey;

    @Column(name = "status", nullable = false)
    private OutboxEventStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    /** Не раньше этого момента событие можно брать в обработку: и отложенный повтор, и аренда захваченного. */
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}
