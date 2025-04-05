package org.vstu.compprehension.models.businesslogic.backend;

import its.model.TypedVariable;
import its.model.definition.DomainModel;
import its.model.nodes.*;
import its.questions.gen.formulations.TemplatingUtils;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.DecisionTreeReasoner;
import its.reasoner.nodes.DecisionTreeTrace;
import its.reasoner.nodes.DecisionTreeTraceElement;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.vstu.compprehension.models.businesslogic.DomainToBackendAdapter;
import org.vstu.compprehension.models.businesslogic.Explanation;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.ViolationEntity;
import org.vstu.compprehension.utils.HyperText;

import java.util.*;
import java.util.stream.Collectors;

import static org.vstu.compprehension.models.businesslogic.domains.Domain.InterpretSentenceResult;

/**
 * A reasoning backend that works with decision-tree based reasoning;<br>
 * Uses {@link DomainModel} objects to encode data, and {@link DecisionTree}s to encode reasoning processes.<br>
 * Domains aiming to support this backend should have a corresponding {@link Interface}
 * @see its.model.DomainSolvingModel
 */
@Primary
@Component
@RequestScope
@Log4j2
public class DecisionTreeReasonerBackend
    implements Backend<DecisionTreeReasonerBackend.Input, DecisionTreeReasonerBackend.Output>
{
    public static String BACKEND_ID = "DTReasoner";

    private static final Map<String, Map<String, String>> utilLoc = Map.ofEntries(
            Pair.of("RU", Map.ofEntries(
                    Pair.of("andAlsoHint", "влияет всё из нижеперечисленного..."),
                    Pair.of("orAlsoHint", "влияет любое из нижеперечисленного...")
            )),
            Pair.of("EN", Map.ofEntries(
                    Pair.of("andAlsoHint", "it is influenced by all of the following..."),
                    Pair.of("orAlsoHint", "it is influenced by any of the following...")
            ))
    );

    @NotNull
    @Override
    public String getBackendId() {
        return BACKEND_ID;
    }

    /**
     * Information, needed to perform a decision-tree based reasoning
     * @param situationDomainModel combined question and domain data, described in the {@link DomainModel} form
     * @param decisionTree a decision tree structure that describes the reasoning process
     */
    public record Input(
        DomainModel situationDomainModel,
        DecisionTree decisionTree
    ){}

    /**
     * Aggregation policy for creating explanation messages
     */
    public enum AggregationPolicy {
        SimAND,
        SimOR,
        Default
    }

    /**
     * Output of a decision-tree based reasoning
     * @param situation the situation object, describing the (possibly) changed state of the source question
     * @param isReasoningDone false, if no reasoning actually happened (see {@link Interface#interpretJudgeNotPerformed}),
     *                       otherwise true
     * @param results if reasoning was actually performed, describes its results
     */
    public record Output(
        LearningSituation situation,
        boolean isReasoningDone,
        DecisionTreeTrace results
    ){}

    public static List<DecisionTreeTraceElement<?, ?>> nestedTraceElements(DecisionTreeTrace trace) {
        ArrayList<DecisionTreeTraceElement<?, ?>> elements = new ArrayList<>();
        _nestedTraceWalk(trace, elements);
        return elements;
    }

    private static void _nestedTraceWalk(DecisionTreeTrace trace, List<DecisionTreeTraceElement<?,?>> elements) {
        for (DecisionTreeTraceElement<?, ?> element : trace) {
            elements.add(element);
            for (DecisionTreeTrace subTrace : Objects.requireNonNullElse(element.nestedTraces(), new ArrayList<DecisionTreeTrace>())) {
                _nestedTraceWalk(subTrace, elements);
            }
        }
    }

    public static Explanation collectExplanationsFromTrace(Explanation.Type type,
                                                            DecisionTreeTrace trace,
                                                            DomainModel domain,
                                                            Language lang) {
        return Explanation.aggregate(type, _collectExplanations(type, trace, null, domain, lang, 1));
    }

    private static List<Explanation> _collectExplanations(Explanation.Type type,
                                     DecisionTreeTrace trace,
                                     Explanation parent,
                                     DomainModel domain,
                                     Language lang,
                                     int level
    ) {
        List<Explanation> traceExplanations = new ArrayList<>();
        for (DecisionTreeTraceElement<?, ?> element : trace) {
            LearningSituation learningSituation = new LearningSituation(domain, element.getVariablesSnapshot());
            if (Objects.requireNonNullElse(element.nestedTraces(), new ArrayList<DecisionTreeTrace>()).isEmpty()
                    && element.getNode() instanceof BranchResultNode res
                    && (type == Explanation.Type.ERROR) != element.getNodeResult().equals(BranchResult.CORRECT)
                    && element.getNode().getMetadata().containsAny("explanation")) {
                traceExplanations.add(Interface.extractExplanation(res,
                        lang.toLocaleString(), learningSituation));
            } else {
                String prefix = Interface.getCommonExplanationPrefix(
                        learningSituation, trace.getResultingNode().getDecisionTree(), type, lang.toLocaleString()
                );
                Explanation newParent = parent;
                if (element.getNode() instanceof AggregationNode agg && agg.getAggregationMethod().equals(AggregationMethod.AND)) {
                    if (type == Explanation.Type.HINT) {
                        if (level != 1) prefix = String.format("<i>%s</i>", utilLoc.get(lang.toLocaleString()).get("andAlsoHint"));
                        newParent = new Explanation(type, prefix);
                        traceExplanations.add(newParent);
                    }
                } else if (element.getNode() instanceof AggregationNode agg && agg.getAggregationMethod().equals(AggregationMethod.OR)) {
                    if (type == Explanation.Type.ERROR) {
                        if (level != 1) prefix = String.format("<i>%s</i>", utilLoc.get(lang.toLocaleString()).get("orAlsoHint"));
                        newParent = new Explanation(type, prefix);
                        traceExplanations.add(newParent);
                    }
                }
                for (DecisionTreeTrace subTrace : Objects.requireNonNullElse(element.nestedTraces(), new ArrayList<DecisionTreeTrace>())) {
                    traceExplanations.addAll(_collectExplanations(type, subTrace, newParent, domain, lang, level + 1));
                }
                if (newParent != null && (newParent.getChildren().isEmpty() || newParent.getChildren().size() == 1)) {
                    traceExplanations.remove(newParent);
                    if (newParent.getChildren().size() == 1) traceExplanations.add(newParent.getChildren().getFirst());
                }
            }
        }

        if (parent == null) {
            return traceExplanations.stream()
                    .filter(e -> !e.isEmpty())
                    .toList();
        } else {
            parent.getChildren().addAll(traceExplanations.stream()
                    .filter(e -> !e.isEmpty())
                    .toList());
            return List.of();
        }
    }

    @Override
    public DecisionTreeReasonerBackend.Output judge(Input questionData) {
        DomainModel situationModel = questionData.situationDomainModel;
        DecisionTree decisionTree = questionData.decisionTree;

        LearningSituation situation = new LearningSituation(
            situationModel,
            LearningSituation.collectDecisionTreeVariables(situationModel)
        );

        if(situation.getDecisionTreeVariables().keySet().containsAll(
            decisionTree.getVariables().stream().map(TypedVariable::getVarName).collect(Collectors.toSet())
        )){
            return new Output(
                situation,
                true,
                DecisionTreeReasoner.solve(decisionTree, situation)
            );
        }

        return new Output(
            situation,
            false,
            null
        );
    }

    @Override
    public DecisionTreeReasonerBackend.Output solve(Input questionData) {
        return null;
    }

    public static abstract class Interface
        extends DomainToBackendAdapter<Input, Output, DecisionTreeReasonerBackend> {

        public Interface() {
            super(DecisionTreeReasonerBackend.class);
        }

        private final static String ERROR_NODE_ATTR = "error";
        @Override
        public InterpretSentenceResult interpretJudgeOutput(
            Question judgedQuestion,
            Output backendOutput
        ) {

            if(!backendOutput.isReasoningDone){
                return interpretJudgeNotPerformed(judgedQuestion, backendOutput.situation);
            }
            List<DecisionTreeTraceElement<?, ?>> traceElements = nestedTraceElements(backendOutput.results);

            InterpretSentenceResult result = new InterpretSentenceResult();
            for (DecisionTreeTraceElement<?,?> res : traceElements) {
                String[] resSkill = res.getNode().getMetadata().containsAny("skill") && res.getNode().getMetadata().get("skill") != null ?
                        res.getNode().getMetadata().get("skill").toString().split(";") : new String[0];
                String[] resLaw = res.getNode().getMetadata().containsAny("law") && res.getNode().getMetadata().get("law") != null ?
                        res.getNode().getMetadata().get("law").toString().split(";") : new String[0];
                Collections.addAll(result.domainSkills, resSkill);
                Collections.addAll(result.domainNegativeLaws, resLaw);
            }

            updateJudgeInterpretationResult(result, backendOutput);

            Language lang = getUserLanguageByQuestion(judgedQuestion);
            result.explanation = collectExplanationsFromTrace(Explanation.Type.ERROR, backendOutput.results,
                    backendOutput.situation.getDomainModel(),
                    lang
            );
            List<ViolationEntity> mistakes = result.explanation.getDomainLawNames()
                    .stream().map(errorName -> {
                        ViolationEntity violation = new ViolationEntity();
                        violation.setLawName(errorName);
                        violation.setViolationFacts(new ArrayList<>());
                        return violation;
                    })
                    .collect(Collectors.toList());
            result.violations = mistakes;
            result.correctlyAppliedLaws = new ArrayList<>();
            result.isAnswerCorrect = mistakes.isEmpty();
            return result;
        }

        /**
         * Get current user's language from a question
         */
        protected Language getUserLanguageByQuestion(Question question){
            try {
                return question.getQuestionData()
                    .getExerciseAttempt()
                    .getUser()
                    .getPreferred_language(); // The language currently selected in UI
            } catch (NullPointerException e) {
                return Language.RUSSIAN/*ENGLISH*/;  // fallback if it cannot be figured out
            }
        }

        /**
         * Create an interpretation result for a situation, in which a reasoning could not be performed
         * (Currently only possible if not all input variables are present)
         * @param judgedQuestion a question which prompted the unfinished judge
         * @param preparedSituation a learning situation that was prepared for this question by {@link #prepareBackendInfoForJudge} 
         */
        protected abstract InterpretSentenceResult interpretJudgeNotPerformed(
            Question judgedQuestion,
            LearningSituation preparedSituation
        );

        /**
         * Update a {@link #judge} interpretation result with domain-specific logic
         * Mainly used on {@link InterpretSentenceResult#IterationsLeft}
         * @param interpretationResult the updated result
         * @param backendOutput the output from the backend's {@link #judge} method
         */
        protected abstract void updateJudgeInterpretationResult(
            InterpretSentenceResult interpretationResult,
            Output backendOutput
        );

        public static String getCommonExplanationPrefix(LearningSituation situation,
                                                        DecisionTree dt,
                                                        Explanation.Type type, String localizationCode) {
            Object meta = dt.getMainBranch().getMetadata().get(localizationCode,
                    (type == Explanation.Type.HINT ? "hint" : "error") + "_prefix");
            String rawPrefix = meta != null ? meta.toString() : "";
            String expanded = TemplatingUtils.interpret(rawPrefix, situation, localizationCode, Map.of());
            {
                if (expanded.contains("операто ")) {
                    // Fix spelling (note the space at the end).
                    expanded = expanded.replaceAll("операто ", "оператор ");
                }
                if (expanded.contains("operato ")) {
                    // Fix spelling (note the space at the end).
                    expanded = expanded.replaceAll("operato ", "operator ");
                }
            }
            return expanded;
        }

        public static Explanation extractExplanation(BranchResultNode resultNode, String localizationCode,
                                                     LearningSituation learningSituation){
            Explanation.Type type = resultNode.getValue() == BranchResult.CORRECT ?
                    Explanation.Type.HINT : Explanation.Type.ERROR;
            Object explanation = resultNode.getMetadata().get(localizationCode, "explanation");
            String prefix = getCommonExplanationPrefix(learningSituation, resultNode.getDecisionTree(), type, localizationCode);
            String explanationTemplate = explanation == null ? "WRONG" : prefix.concat(explanation.toString());
            String expanded = TemplatingUtils.interpret(explanationTemplate, learningSituation, localizationCode, Map.of());
            {
                if (expanded.contains("операто ")) {
                    // Fix spelling (note the space at the end).
                    expanded = expanded.replaceAll("операто ", "оператор ");
                }
                if (expanded.contains("operato ")) {
                    // Fix spelling (note the space at the end).
                    expanded = expanded.replaceAll("operato ", "operator ");
                }
            }
            Explanation expl = new Explanation(type, expanded);
            if (resultNode.getMetadata().containsAny("skill")) {
                String skillName = resultNode.getMetadata().getString("skill");
                expl.setCurrentDomainLawName(skillName);
            }
            return expl;
        }

        @Override
        public void updateQuestionAfterSolve(
            Question question,
            Output backendOutput
        ) {}
    }
}
