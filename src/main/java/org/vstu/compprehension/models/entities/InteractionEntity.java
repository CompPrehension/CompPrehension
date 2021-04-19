package org.vstu.compprehension.models.entities;

import lombok.val;
import org.vstu.compprehension.models.entities.EnumData.InteractionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Interaction")
public class InteractionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orderNumber")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int orderNumber;


    @Column(name = "interactionType")
    @Enumerated(EnumType.STRING)
    private InteractionType interactionType;


    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "feedback_id", referencedColumnName = "id")
    @NotFound(action = NotFoundAction.IGNORE)
    private FeedbackEntity feedback;

    @OneToMany(mappedBy = "interaction", cascade = CascadeType.ALL)
    private List<ViolationEntity> violations;

    @OneToMany(mappedBy = "interaction", cascade = CascadeType.ALL)
    private List<CorrectLawEntity> correctLaw;

    @OneToMany(mappedBy = "interaction", fetch = FetchType.LAZY)
    private List<ResponseEntity> responses;


    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    public InteractionEntity(
            QuestionEntity question,
            List<ViolationEntity> violations,
            List<String> correctlyAppliedLaws,
            List<ResponseEntity> responses){
        this.setQuestion(question);
        this.setInteractionType(InteractionType.SEND_RESPONSE);//Какой нужен?
        this.setFeedback(new FeedbackEntity());

        this.setViolations(new ArrayList<>(violations));
        for(val m : this.getViolations()) {
            m.setInteraction(this);
        }

        this.setResponses(new ArrayList<>(responses));
        for(val r : this.getResponses()) {
            r.setInteraction(this);
        }

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
