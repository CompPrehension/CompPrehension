package org.vstu.compprehension.entities;

import org.vstu.compprehension.data.questionoptions.QuestionOptionsData;
import org.vstu.compprehension.data.question.BackendFactData;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.enums.QuestionStatus;
import org.vstu.compprehension.data.enums.QuestionType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity @Getter @Setter
@NoArgsConstructor
@Table(name = "Question")
public class QuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.ORDINAL)
    private QuestionType questionType;

    @Enumerated(EnumType.ORDINAL)
    private QuestionStatus questionStatus;

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "question_name", length = 255)
    private String questionName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metadata_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @Nullable
    private QuestionMetadataEntity metadata;

    /**
     * Kind of question within Domain
     */
    @Column(name = "question_domain_type")
    private String questionDomainType;

    @Type(JsonType.class)
    @Column(name = "options_json", columnDefinition = "json")
    private QuestionOptionsData options;

    @Type(JsonType.class)
    @Column(name = "tags", columnDefinition = "json", nullable = false)
    @NotNull
    private List<String> tags = new ArrayList<>(0);

    @ToString.Exclude
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    @OrderBy("answerId")
    private List<AnswerObjectEntity> answerObjects;

    @ToString.Exclude
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private @NotNull List<InteractionEntity> interactions = new ArrayList<>(0);

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_request_id", nullable = true)
    private @Nullable QuestionRequestLogEntity questionRequestLog;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "exercise_attempt_id", nullable = true)
    @Nullable
    private ExerciseAttemptEntity exerciseAttempt;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_name", nullable = false)
    private DomainEntity domainEntity;

    @Type(JsonType.class)
    @Column(name = "statement_facts", nullable = false)
    @Basic(fetch = FetchType.LAZY)
    private List<BackendFactData> statementFacts = new ArrayList<>();

    @Type(JsonType.class)
    @Column(name = "solution_facts", nullable = false)
    @Basic(fetch = FetchType.LAZY)
    private List<BackendFactData> solutionFacts = new ArrayList<>();
}
