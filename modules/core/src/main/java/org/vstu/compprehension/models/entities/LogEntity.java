package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.vstu.compprehension.models.entities.shared.BaseEntity;
import org.vstu.compprehension.models.entities.shared.CreateTrackedEntity;

import java.util.Date;

@Entity
@Table(name = "Logs", indexes = {
    @Index(name = "idx_date", columnList = "date DESC"),
    @Index(name = "idx_app", columnList = "app"),
    @Index(name = "idx_level", columnList = "level")
})
public class LogEntity implements CreateTrackedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = true, length = 36)
    private String requestId;

    @Column(nullable = true, length = 36)
    private String sessionId;

    @Column(nullable = true, length = 36)
    private String userId;

    @Getter @Setter
    @Column(nullable = true, name = "date", columnDefinition="DATETIME(6)")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(nullable = true, length = 50)
    private String app;

    @Column(nullable = true, length = 10)
    private String level;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String payload;
}
