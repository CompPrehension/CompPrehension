package org.vstu.compprehension.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;
import org.vstu.compprehension.data.question.InteractionReasoningData;
import org.vstu.compprehension.enums.InteractionType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "interaction")
public class InteractionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false)
    private int orderNumber;

    @Column(name = "interaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private InteractionType interactionType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    // Связь, намеренно оставленная EAGER. 
    // @NotFound(IGNORE)в Hibernate несовместим с ленивой загрузкой.
    // TODO убрать @NotFound и починить висячие ссылки на feedback в данных.
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "feedback_id", referencedColumnName = "id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private FeedbackEntity feedback;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    @Type(JsonType.class)
    @Column(name = "reasonings", nullable = false)
    private List<InteractionReasoningData> reasonings;

    @ToString.Exclude
    @OneToOne(mappedBy = "interaction", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private InteractionClarificationEntity clarification;

    @ToString.Exclude
    @OneToMany(mappedBy = "interaction", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<ResponseEntity> responses;

    @ToString.Exclude
    @OneToMany(mappedBy = "createdByInteraction", fetch = FetchType.LAZY)
    private List<ResponseEntity> newResponses;
    
    @ToString.Exclude
    @OneToMany(mappedBy = "mainQuestionInteraction", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<SupplementaryStepEntity> relatedSupplementarySteps;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    public InteractionEntity(
            InteractionType type,
            QuestionEntity question,
            boolean isCorrect,
            List<InteractionReasoningData> reasonings,
            InteractionClarificationEntity clarification,
            List<ResponseEntity> allResponses,
            List<ResponseEntity> newResponses){
        this.setQuestion(question);
        this.setInteractionType(type);
        this.setFeedback(new FeedbackEntity());
        this.setCorrect(isCorrect);
        this.setReasonings(List.copyOf(reasonings));

        this.setClarification(clarification);
        if (clarification != null) {
            clarification.setInteraction(this);
        }

        this.setResponses(new ArrayList<>(allResponses));
        for(val r : this.getResponses()) {
            r.setInteraction(this);
        }

        this.setNewResponses(new ArrayList<>(newResponses));
        for(val r : this.getNewResponses()) {
            r.setCreatedByInteraction(this);
        }
    }
}
