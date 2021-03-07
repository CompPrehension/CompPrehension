package com.example.demo.models.entities;

import com.example.demo.models.entities.EnumData.QuestionStatus;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.models.entities.QuestionOptions.QuestionOptionsEntity;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
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

    @Column(name = "question_domain_type")
    private String questionDomainType;

    @Column(name = "answers_require_context")
    private Boolean areAnswersRequireContext;

    @Type(type = "json")
    @Column(name = "options_json", columnDefinition = "json")
    private QuestionOptionsEntity options;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<AnswerObjectEntity> answerObjects;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<InteractionEntity> interactions;

    @ManyToOne
    @JoinColumn(name = "exerciseAttempt_id", nullable = false)
    private ExerciseAttemptEntity exerciseAttempt;

    @ManyToOne
    @JoinColumn(name = "domain_name", nullable = false)
    private DomainEntity domainEntity;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<BackendFactEntity> statementFacts;
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<BackendFactEntity> solutionFacts;
}
