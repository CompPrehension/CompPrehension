package org.vstu.compprehension.models.entities;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.entities.EnumData.QuestionStatus;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity @Getter @Setter
@NoArgsConstructor
@Table(name = "Question")
@TypeDef(name = "json", typeClass = JsonStringType.class)
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

    @Type(type = "json")
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

    @Type(type = "json")
    @Column(name = "statement_facts", nullable = false)
    @Basic(fetch = FetchType.LAZY)
    private List<BackendFactEntity> statementFacts = new ArrayList<>();

    @Type(type = "json")
    @Column(name = "solution_facts", nullable = false)
    @Basic(fetch = FetchType.LAZY)
    private List<BackendFactEntity> solutionFacts = new ArrayList<>();
}
