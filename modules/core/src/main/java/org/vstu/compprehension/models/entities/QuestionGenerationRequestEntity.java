package org.vstu.compprehension.models.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;

import java.util.Date;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "question_generation_requests", indexes = {
    @Index(name = "question_generation_requests_search_idx", columnList = "domain_shortname,is_completed,created_at,questions_to_generate")
})
public class QuestionGenerationRequestEntity {
    public QuestionGenerationRequestEntity(QuestionBankSearchRequest questionRequest, int questionsToGenerate, Long exerciseAttemptId) {
        this.questionRequest = questionRequest;
        this.questionsToGenerate = questionsToGenerate;
        this.exerciseAttemptId = exerciseAttemptId;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;
    
    @Column(name = "is_completed", nullable = false, columnDefinition = "boolean default false")
    private boolean isCompleted = false;

    @Type(JsonType.class)
    @Column(name = "question_request", columnDefinition = "json", nullable = false)
    private QuestionBankSearchRequest questionRequest;
    
    @Column(name = "questions_to_generate", nullable = false)
    private int questionsToGenerate = 0;
    
    @Column(name = "processing_attempts", nullable = false)
    private int processingAttempts = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_attempt_id", referencedColumnName = "id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ExerciseAttemptEntity exerciseAttempt;

    @Column(name = "exercise_attempt_id")
    private Long exerciseAttemptId;

    //region generated columns
    
    @Column(name = "domain_shortname",
            columnDefinition = "varchar(255) GENERATED ALWAYS AS (question_request->>\"$.domainShortname\") VIRTUAL NOT NULL",
            insertable = false, updatable = false)
    private String domainShortname;
    
    @Column(name = "denied_concepts_bitmask",
            columnDefinition = "bigint GENERATED ALWAYS AS (question_request->>\"$.deniedConceptsBitmask\") VIRTUAL NOT NULL",
            insertable = false, updatable = false)
    private Long deniedConceptsBitmask;
    
    @Column(name = "target_concepts_bitmask",
            columnDefinition = "bigint GENERATED ALWAYS AS (question_request->>\"$.targetConceptsBitmask\") VIRTUAL NOT NULL",
            insertable = false, updatable = false)
    private Long targetConceptsBitmask;
    
    @Column(name = "target_laws_bitmask",
            columnDefinition = "bigint GENERATED ALWAYS AS (question_request->>\"$.targetLawsBitmask\") VIRTUAL NOT NULL",
            insertable = false, updatable = false)
    private Long targetLawsBitmask;
    
    @Column(name = "denied_laws_bitmask",
            columnDefinition = "bigint GENERATED ALWAYS AS (question_request->>\"$.deniedLawsBitmask\") VIRTUAL NOT NULL",
            insertable = false, updatable = false)
    private Long deniedLawsBitmask;
    
    @Column(name = "target_tags_bitmask",
            columnDefinition = "bigint GENERATED ALWAYS AS (question_request->>\"$.targetTagsBitmask\") VIRTUAL NOT NULL",
            insertable = false, updatable = false)
    private Long targetTagsBitmask;
    
    @Column(name = "complexity",
            columnDefinition = "float GENERATED ALWAYS AS (question_request->>\"$.complexity\") VIRTUAL NOT NULL",
            insertable = false, updatable = false)    
    private Float complexity;
    
    @Column(name = "steps_min",
            columnDefinition = "int GENERATED ALWAYS AS (question_request->>\"$.stepsMin\") VIRTUAL NOT NULL",
            insertable = false, updatable = false)
    private Integer stepsMin;
    
    @Column(name = "steps_max",
            columnDefinition = "int GENERATED ALWAYS AS (question_request->>\"$.stepsMax\") VIRTUAL NOT NULL",
            insertable = false, updatable = false)
    private Integer stepsMax;
    
    //endregion
}
