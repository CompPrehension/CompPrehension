package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.entities.EnumData.QuestionStatus;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;

import java.util.ArrayList;
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

    /**
     * Kind of question within Domain
     */
    @Column(name = "question_domain_type")
    private String questionDomainType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", columnDefinition = "json")
    private QuestionOptionsEntity options;

    @ToString.Exclude
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    @OrderBy("answerId")
    private List<AnswerObjectEntity> answerObjects;

    @ToString.Exclude
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private @NotNull List<InteractionEntity> interactions = new ArrayList<>(0);

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "exerciseAttempt_id", nullable = false)
    private ExerciseAttemptEntity exerciseAttempt;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "domain_name", nullable = false)
    private DomainEntity domainEntity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "statement_facts", nullable = false)
    @Basic(fetch = FetchType.LAZY)
    private List<BackendFactEntity> statementFacts = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "solution_facts", nullable = false)
    @Basic(fetch = FetchType.LAZY)
    private List<BackendFactEntity> solutionFacts = new ArrayList<>();
}
