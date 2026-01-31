package org.vstu.compprehension.models.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.entities.shared.BaseEntity;
import org.vstu.compprehension.models.entities.shared.CreateTrackedEntity;
import org.vstu.compprehension.models.entities.shared.TimeTrackingListener;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "questions_meta_search_requests", indexes = {
    @Index(name = "idx_qmsr_created_at", columnList = "created_at"),
    @Index(name = "idx_qmsr_question_request_id", columnList = "question_request_id"),
    @Index(name = "idx_qmsr_quality", columnList = "quality"),
})
@EntityListeners(TimeTrackingListener.class)
public class QuestionMetadataSearchRequestEntity extends BaseEntity implements CreateTrackedEntity {
    public enum Quality {
        BestUnused,
        BestUsed,
        Normal,
        Relaxed,
    }
    public record Iteration(@NotNull Quality quality, @NotNull Integer limit, @NotNull Integer found){ };
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_request_id", nullable = true)
    private UUID questionRequestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "quality", nullable = false)
    private Quality quality;

    @Column(name = "qlimit", nullable = false) // limit заразервировано mysql
    private int limit;

    @Column(name = "found", nullable = false)
    private int found;

    @Type(JsonType.class)
    @Column(name = "search_request", nullable = false, columnDefinition = "json")
    private QuestionBankSearchRequest searchRequest;

    @Type(JsonType.class)
    @Column(name = "search_iterations", nullable = false, columnDefinition = "json")
    private List<Iteration> iterations;

    public QuestionMetadataSearchRequestEntity(@NotNull QuestionBankSearchRequest searchRequest, @NotNull List<Iteration> iterations, @Nullable UUID questionRequestId) {
        if (iterations.isEmpty()){
            throw new IllegalArgumentException("The iterations list must not be empty: at least one iteration is required to initialize QuestionMetadataSearchRequestEntity.");
        }
        this.searchRequest = searchRequest;
        this.iterations = iterations;

        var lastIteration = iterations.getLast();
        this.found = lastIteration.found;
        this.quality = lastIteration.quality;
        this.limit = lastIteration.limit;
        this.questionRequestId = questionRequestId;
    }
}
