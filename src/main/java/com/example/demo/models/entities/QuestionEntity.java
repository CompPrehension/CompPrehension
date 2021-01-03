package com.example.demo.models.entities;

import com.example.demo.models.entities.EnumData.QuestionStatus;
import com.example.demo.models.entities.EnumData.QuestionType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
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

    @Column(name = "question_text")
    private String questionText;

    @Column(name = "question_domain_type")
    private String questionDomainType;

    @Column(name = "answers_require_context")
    private Boolean areAnswersRequireContext;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<QuestionAttemptEntity> questionAttempts;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<AnswerObjectEntity> answerObjects;

    @ManyToOne
    @JoinColumn(name = "domain_name", nullable = false)
    private DomainEntity domainEntity;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<BackendFactEntity> statementFacts;
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<BackendFactEntity> solutionFacts;
}
