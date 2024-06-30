package org.vstu.compprehension.models.businesslogic.backend;

import its.model.TypedVariable;
import its.model.definition.Domain;
import its.model.definition.MetadataProperty;
import its.model.nodes.BranchResultNode;
import its.model.nodes.DecisionTree;
import its.questions.gen.QuestioningSituation;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.DecisionTreeReasoner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.vstu.compprehension.models.businesslogic.DTLaw;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.util.ReasoningOptions;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.ViolationEntity;
import org.vstu.compprehension.utils.HyperText;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO Class Description
 *
 * @author Marat Gumerov
 * @since 28.01.2024
 */
@Primary
@Component
@RequestScope
@Log4j2
public class DecisionTreeReasonerBackend implements Backend {
    public static String BACKEND_ID = "DTReasoner";

    @NotNull
    @Override
    public String getBackendId() {
        return BACKEND_ID;
    }

    @Override
    public Collection<Fact> solve(
        List<Law> laws,
        List<BackendFactEntity> statement,
        ReasoningOptions reasoningOptions
    ) {
        throw new NotImplementedException();
    }

    @Override
    public Collection<Fact> solve(
        List<Law> laws,
        Collection<Fact> statement,
        ReasoningOptions reasoningOptions
    ) {
        if (1==1) {
            // early exit (no-op).
            return new ArrayList<>(statement);
        }

        DecisionTree decisionTree = laws.stream()
            .filter(l -> l instanceof DTLaw)
            .findFirst()
            .map(l -> ((DTLaw)l).getDecisionTree())
            .orElseThrow();

        Domain situationModel = statement.stream()
            .filter(f -> f instanceof DomainFact)
            .findFirst()
            .map(f -> ((DomainFact) f).getDomain())
            .orElseThrow();

        LearningSituation situation = new LearningSituation(situationModel, new HashMap<>());

//        DecisionTreeReasoner.solve(decisionTree, situation);

        List<Fact> solution = new ArrayList<>();
        solution.add(new DomainFact(situation.getDomain()));
        return solution;
    }

    @Override
    public Collection<Fact> judge(
        List<Law> laws,
        List<BackendFactEntity> statement,
        List<BackendFactEntity> correctAnswer,
        List<BackendFactEntity> response,
        ReasoningOptions reasoningOptions
    ) {
        throw new NotImplementedException();
    }

    @Override
    public Collection<Fact> judge(
        List<Law> laws,
        Collection<Fact> statement,
        Collection<Fact> correctAnswer,
        Collection<Fact> response,
        ReasoningOptions reasoningOptions
    ) {
        DecisionTree decisionTree = laws.stream()
            .filter(l -> l instanceof DTLaw)
            .findFirst()
            .map(l -> ((DTLaw)l).getDecisionTree())
            .orElseThrow();

        Domain situationModel = statement.stream()
            .filter(f -> f instanceof DomainFact)
            .findFirst()
            .map(f -> ((DomainFact) f).getDomain())
            .orElseThrow();

        LearningSituation situation = new LearningSituation(
            situationModel,
            LearningSituation.collectDecisionTreeVariables(situationModel)
        );


        List<Fact> reasonerOutput = new ArrayList<>();
        if(situation.getDecisionTreeVariables().keySet().containsAll(
            decisionTree.getVariables().stream().map(TypedVariable::getVarName).collect(Collectors.toSet())
        )){
            List<DecisionTreeReasoner.DecisionTreeEvaluationResult> judgeResults =
                DecisionTreeReasoner.solve(decisionTree, situation);

            int i = 0;
            for(var result: judgeResults){
                reasonerOutput.addAll(reasonerOutputToFacts(result, ++i));
            }
        } else {
            log.warn("Required Decision Tree variables are NOT SET !");
        }

        reasonerOutput.add(new DomainFact(situation.getDomain()));
        return reasonerOutput;
    }

    private final static String ERROR_NODE_ATTR = "errorNode";
    private static List<Fact> reasonerOutputToFacts(DecisionTreeReasoner.DecisionTreeEvaluationResult result, int i){
        List<Fact> facts = new ArrayList<>();
        BranchResultNode resultNode = result.getNode();
        String nodeId = (String) resultNode.getMetadata().get(ERROR_NODE_ATTR);
        if(resultNode.getValue()) return facts; //игнорируем зеленые узлы
        if (nodeId == null) return facts;

        facts.add(new EvaluationResultFact(result));
        facts.add(new Fact(String.valueOf(i), ERROR_NODE_ATTR, nodeId));
        result.getVariablesSnapshot().forEach((varName, obj) -> {
            facts.add(new Fact(String.valueOf(i), "var", varName + " = " + obj.getObjectName()));
        });
        return facts;
    }

    public static List<ViolationEntity> reasonerOutputFactsToViolations(List<Fact> reasonerOutputFacts){
        Map<String, List<Fact>> groupedByResults = reasonerOutputFacts.stream()
            .collect(Collectors.groupingBy(fact -> fact.getSubject() == null ? "" : fact.getSubject()));

        List<ViolationEntity> violations = new ArrayList<>();
        groupedByResults.forEach((i, facts) -> {
            if (i.isEmpty()) {  // ignore facts having empty subject.
                return;
            }
            ViolationEntity violation = new ViolationEntity();
            facts.stream()
                .filter(fact -> ERROR_NODE_ATTR.equals(fact.getVerb()))
                .findFirst().ifPresent(fact -> violation.setLawName(fact.getObject()));
            violation.setViolationFacts(Fact.factsToEntities(facts));
            violations.add(violation);
        });
        return violations;
    }

    public static List<HyperText> makeExplanations(List<Fact> reasonerOutputFacts, Language lang){

        List<Fact> describingFacts = reasonerOutputFacts.stream()
            .filter(fact -> fact.getSubject() == null)
            .toList(); //FIXME?

        Domain situation = describingFacts.stream()
            .filter(f -> f instanceof DomainFact)
            .findFirst().map(f -> ((DomainFact) f).getDomain())
            .orElseThrow();

        List<DecisionTreeReasoner.DecisionTreeEvaluationResult> results = describingFacts.stream()
            .filter(f -> f instanceof EvaluationResultFact)
            .map(f -> ((EvaluationResultFact)f).getEvaluationResult())
            .toList();

        QuestioningSituation textSituation = new QuestioningSituation(situation, lang.toLocaleString());

        List<HyperText> explanations = new ArrayList<>();
        for(var result : results){
            textSituation.getDecisionTreeVariables().clear();
            textSituation.getDecisionTreeVariables().putAll(result.getVariablesSnapshot());
            explanations.add(new HyperText(getExplanation(result.getNode(), textSituation)));
        }
        return explanations;
    }

    private static String getExplanation(BranchResultNode resultNode, QuestioningSituation textSituation){
        Object explanation = resultNode.getMetadata().get(
            new MetadataProperty("explanation") //FIXME
        );
        String explanationTemplate = explanation == null ? "WRONG" : explanation.toString();
        return textSituation.getTemplating().interpret(explanationTemplate);
    }

    @AllArgsConstructor
    @Getter
    public static class DomainFact extends Fact {
        private final Domain domain;
    }

    @AllArgsConstructor
    @Getter
    public static class EvaluationResultFact extends Fact {
        private final DecisionTreeReasoner.DecisionTreeEvaluationResult evaluationResult;
    }
}
