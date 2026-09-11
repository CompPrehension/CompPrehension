package org.vstu.compprehension.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.vstu.compprehension.data.question.SupplementarySituationData;


@Entity @Getter @Setter
@NoArgsConstructor
@Table(name = "SupplementaryStep")
public class SupplementaryStepEntity {    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "main_question_interaction_id", nullable = false)
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
