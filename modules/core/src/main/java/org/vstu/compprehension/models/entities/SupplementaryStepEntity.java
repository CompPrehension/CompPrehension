package org.vstu.compprehension.models.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import its.model.definition.DomainModel;
import its.model.definition.ObjectRef;
import its.model.nodes.BranchResult;
import its.questions.gen.QuestioningSituation;
import its.reasoner.LearningSituation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

import java.util.Map;
import java.util.stream.Collectors;

@Entity @Getter @Setter
@NoArgsConstructor
@Table(name = "SupplementaryStep")
public class SupplementaryStepEntity {
    
    public SupplementaryStepEntity(InteractionEntity mainQuestionInteraction, QuestioningSituation situation, QuestionEntity supplementaryQuestion, Integer nextStateId){
        this.mainQuestionInteraction = mainQuestionInteraction;
        this.situationInfo = new SupplementarySituation(situation);
        this.supplementaryQuestion = supplementaryQuestion;
        this.nextStateId = nextStateId;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    public static class SupplementarySituation{
        private Map<String, String> reasoningVariables;
        private Map<String, String> discussedVariables;
        private Map<Integer, Integer> givenAnswers;
        private Map<String, BranchResult> assumedResults;
        private String localizationCode;
        
        public SupplementarySituation(QuestioningSituation situation){
            this.reasoningVariables = situation.getDecisionTreeVariables()
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getObjectName()));
            this.discussedVariables = situation.getDiscussedVariables();
            this.givenAnswers = situation.getGivenAnswers();
            this.assumedResults = situation.getAssumedResults();
            this.localizationCode = situation.getLocalizationCode();
        }

        public QuestioningSituation toQuestioningSituation(DomainModel situationModel) {
            Map<String, ObjectRef> vars = reasoningVariables
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new ObjectRef(e.getValue())));
            vars.putAll(LearningSituation.collectDecisionTreeVariables(situationModel));
            return new QuestioningSituation(situationModel, vars, discussedVariables, givenAnswers, assumedResults, localizationCode);
        }
    }
    
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "main_question_interaction_id")
    private InteractionEntity mainQuestionInteraction;
    
    @ToString.Exclude
    @OneToOne
    @JoinColumn(name = "supplementary_question_id")
    private QuestionEntity supplementaryQuestion;

    @Type(JsonType.class)
    @Column(name = "situation_info", nullable = false)
    private SupplementarySituation situationInfo;
    
    @Column(name = "next_state_id")
    private Integer nextStateId;
}
