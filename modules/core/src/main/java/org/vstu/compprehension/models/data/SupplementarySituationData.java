package org.vstu.compprehension.models.data;

import its.model.definition.DomainModel;
import its.model.definition.ObjectRef;
import its.model.nodes.BranchResult;
import its.questions.gen.QuestioningSituation;
import its.reasoner.LearningSituation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Снимок ситуации, в которой задан вспомогательный вопрос.
 * <p>
 * Раньше был вложенным классом {@code SupplementaryStepEntity} и потому выглядел как часть
 * слоя хранения, хотя на деле это значение из json-колонки. Хранение не изменилось:
 * сериализация идёт по полям, а их имена те же.
 */
@Getter
@Setter
@NoArgsConstructor
public class SupplementarySituationData {
    private Map<String, String> reasoningVariables;
    private Map<String, String> discussedVariables;
    private Map<Integer, Integer> givenAnswers;
    private Map<String, BranchResult> assumedResults;
    private String localizationCode;

    public SupplementarySituationData(QuestioningSituation situation) {
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
