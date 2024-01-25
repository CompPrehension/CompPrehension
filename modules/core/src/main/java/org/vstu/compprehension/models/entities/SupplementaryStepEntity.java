package org.vstu.compprehension.models.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import its.model.definition.ObjectRef;
import its.questions.gen.QuestioningSituation;
import its.reasoner.LearningSituation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.jena.rdf.model.Model;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;

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
        private Map<String, ObjectRef> reasoningVariables;
        private Map<String, String> discussedVariables;
        private Map<Integer, Integer> givenAnswers;
        private Map<String, Boolean> assumedResults;
        private String localizationCode;
        
        public SupplementarySituation(QuestioningSituation situation){
            this.reasoningVariables = situation.getDecisionTreeVariables();
            this.discussedVariables = situation.getDiscussedVariables();
            this.givenAnswers = situation.getGivenAnswers();
            this.assumedResults = situation.getAssumedResults();
            this.localizationCode = situation.getLocalizationCode();
        }
        
        public QuestioningSituation toQuestioningSituation(its.model.definition.Domain situationModel){
            Map<String, ObjectRef> vars = new HashMap<>(reasoningVariables);
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
