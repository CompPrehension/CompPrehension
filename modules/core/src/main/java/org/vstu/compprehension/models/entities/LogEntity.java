package org.vstu.compprehension.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "Logs")
public class LogEntity {
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

    @Column(nullable = true, columnDefinition="DATETIME(6)")
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    @Column(nullable = true, length = 10)
    private String level;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String payload;
}
