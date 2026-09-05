package org.vstu.compprehension.models.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import its.questions.gen.QuestioningSituation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.vstu.compprehension.models.data.SupplementarySituationData;


@Entity @Getter @Setter
@NoArgsConstructor
@Table(name = "SupplementaryStep")
public class SupplementaryStepEntity {
    
    public SupplementaryStepEntity(InteractionEntity mainQuestionInteraction, QuestioningSituation situation, QuestionEntity supplementaryQuestion, Integer nextStateId){
        this.mainQuestionInteraction = mainQuestionInteraction;
        this.situationInfo = new SupplementarySituationData(situation);
        this.supplementaryQuestion = supplementaryQuestion;
        this.nextStateId = nextStateId;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_question_interaction_id")
    private InteractionEntity mainQuestionInteraction;
    
    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplementary_question_id")
    private QuestionEntity supplementaryQuestion;

    @Type(JsonType.class)
    @Column(name = "situation_info", nullable = false)
    private SupplementarySituationData situationInfo;
    
    @Column(name = "next_state_id")
    private Integer nextStateId;
}
