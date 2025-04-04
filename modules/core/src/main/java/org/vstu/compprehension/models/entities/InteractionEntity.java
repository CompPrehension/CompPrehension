package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.vstu.compprehension.models.entities.EnumData.InteractionType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "Interaction")
public class InteractionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orderNumber")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int orderNumber;

    @Column(name = "lastSupplementaryQuestion")
    private String lastSupplementaryQuestion;

    @Column(name = "interactionType")
    @Enumerated(EnumType.STRING)
    private InteractionType interactionType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "feedback_id", referencedColumnName = "id")
    @NotFound(action = NotFoundAction.IGNORE)
    private FeedbackEntity feedback;

    @ToString.Exclude
    @OneToMany(mappedBy = "interaction", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ViolationEntity> violations;

    @ToString.Exclude
    @OneToMany(mappedBy = "interaction", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CorrectLawEntity> correctLaw;

    @ToString.Exclude
    @OneToMany(mappedBy = "interaction", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ResponseEntity> responses;

    @ToString.Exclude
    @OneToMany(mappedBy = "createdByInteraction", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ResponseEntity> newResponses;
    
    @ToString.Exclude
    @OneToMany(mappedBy = "mainQuestionInteraction", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<SupplementaryStepEntity> relatedSupplementarySteps;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    public InteractionEntity(
            InteractionType type,
            QuestionEntity question,
            List<ViolationEntity> violations,
            List<String> correctlyAppliedLaws,
            List<ResponseEntity> allResponses,
            List<ResponseEntity> newResponses){
        this.setQuestion(question);
        this.setInteractionType(type);
        this.setFeedback(new FeedbackEntity());

        this.setViolations(new ArrayList<>(violations));
        for(val m : this.getViolations()) {
            m.setInteraction(this);
        }

        this.setResponses(new ArrayList<>(allResponses));
        for(val r : this.getResponses()) {
            r.setInteraction(this);
        }

        this.setNewResponses(new ArrayList<>(newResponses));
        for(val r : this.getNewResponses()) {
            r.setCreatedByInteraction(this);
        }

        if(correctlyAppliedLaws == null){
            this.setCorrectLaw(new ArrayList<>());
        } else {
            val correctLaw = correctlyAppliedLaws.stream()
                    .map(correctlyAppliedLaw -> {
                        CorrectLawEntity cle = new CorrectLawEntity();
                        cle.setLawName(correctlyAppliedLaw);
                        cle.setInteraction(this);
                        return cle;
                    })
                    .collect(Collectors.toList());
            this.setCorrectLaw(correctLaw);
        }
    }
}
