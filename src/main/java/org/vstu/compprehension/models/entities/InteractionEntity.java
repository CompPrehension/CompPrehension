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


    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "feedback_id", referencedColumnName = "id")
    @NotFound(action = NotFoundAction.IGNORE)
    private FeedbackEntity feedback;

    @OneToMany(mappedBy = "interaction")
    private List<MistakeEntity> mistakes;

    @OneToMany(mappedBy = "interaction")
    private List<CorrectLawEntity> correctLaw;

    @OneToMany(mappedBy = "interaction", fetch = FetchType.LAZY)
    private List<ResponseEntity> responses;


    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    public InteractionEntity(
            QuestionEntity question,
            List<MistakeEntity> mistakes,
            int IterationsLeft,
            List<String> correctlyAppliedLaws,
            List<ResponseEntity> responses){
        //Сохранение интеракции
        this.setQuestion(question);
        this.setInteractionType(InteractionType.SEND_RESPONSE);//Какой нужен?
        this.setMistakes(mistakes);
        this.setOrderNumber(IterationsLeft);//Показатель порядка?
        this.setResponses(responses);
        for(val r : responses) {
            r.setInteraction(this);
        }
        //ie.setFeedback();Где взять?
        ArrayList<CorrectLawEntity> cles = new ArrayList<>();
        for(int i = 0; i < correctlyAppliedLaws.size(); i++){
            CorrectLawEntity cle = new CorrectLawEntity();
            cle.setLawName(correctlyAppliedLaws.get(i));
           // cle.setInteraction(ie);
            cles.add(cle);
        }
        this.setCorrectLaw(cles);
    }


}
