package org.vstu.compprehension.models.businesslogic.backend;

import its.model.TypedVariable;
import its.model.definition.Domain;
import its.model.nodes.DecisionTree;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.DecisionTreeReasoner;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.rdf.model.Model;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.vstu.compprehension.models.businesslogic.DTLaw;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.backend.facts.DTDomainDescriptionFact;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.util.ReasoningOptions;
import org.vstu.compprehension.models.businesslogic.domains.helpers.ProgrammingLanguageExpressionRDFTransformer;
import org.vstu.compprehension.models.entities.BackendFactEntity;

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
        DecisionTree decisionTree = laws.stream()
            .filter(l -> l instanceof DTLaw)
            .findFirst()
            .map(l -> ((DTLaw)l).getDecisionTree())
            .orElseThrow();

        Domain situationModel = statement.stream()
            .filter(f -> f instanceof DTDomainDescriptionFact)
            .findFirst()
            .map(f -> ((DTDomainDescriptionFact) f).getDomain())
            .orElseThrow();

        LearningSituation situation = new LearningSituation(situationModel, new HashMap<>());

//        DecisionTreeReasoner.solve(decisionTree, situation);

        List<Fact> solution = new ArrayList<>();
        solution.add(new DTDomainDescriptionFact(situation.getDomain()));
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
            .filter(f -> f instanceof DTDomainDescriptionFact)
            .findFirst()
            .map(f -> ((DTDomainDescriptionFact) f).getDomain())
            .orElseThrow();

        LearningSituation situation = new LearningSituation(
            situationModel,
            LearningSituation.collectDecisionTreeVariables(situationModel)
        );

        boolean isCorrect = false;
        if(situation.getDecisionTreeVariables().keySet().containsAll(
            decisionTree.getVariables().stream().map(TypedVariable::getVarName).collect(Collectors.toSet())
        )){
            List<DecisionTreeReasoner.DecisionTreeEvaluationResult> judgeResults =
                DecisionTreeReasoner.solve(decisionTree, situation);

            isCorrect = judgeResults.get(judgeResults.size() - 1).getNode().getValue();
        }

        List<Fact> solution = new ArrayList<>();
        solution.add(new Fact("answer", "isCorrect", String.valueOf(isCorrect)));
        return solution;
    }
}
