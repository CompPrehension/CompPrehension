package org.vstu.compprehension.models.businesslogic.backend;

import its.model.TypedVariable;
import its.model.definition.Domain;
import its.model.definition.MetadataProperty;
import its.model.nodes.BranchResultNode;
import its.model.nodes.DecisionTree;
import its.questions.gen.QuestioningSituation;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.DecisionTreeReasoner;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.ViolationEntity;
import org.vstu.compprehension.utils.HyperText;

import java.util.*;
import java.util.stream.Collectors;

import static org.vstu.compprehension.models.businesslogic.domains.Domain.InterpretSentenceResult;

/**
 * A reasoning backend that works with decision-tree based reasoning;<br>
 * Uses {@link Domain} objects to encode data, and {@link DecisionTree}s to encode reasoning processes.<br>
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

    @NotNull
    @Override
    public String getBackendId() {
        return BACKEND_ID;
    }

    /**
     * Information, needed to perform a decision-tree based reasoning
     * @param situationDomain combined question and domain data, described in the {@link Domain} form
     * @param decisionTree a decision tree structure that describes the reasoning process
     */
    public record Input(
        Domain situationDomain,
        DecisionTree decisionTree
    ){}

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
        List<DecisionTreeReasoner.DecisionTreeEvaluationResult> results
    ){}

    @Override
    public DecisionTreeReasonerBackend.Output judge(Input questionData) {
        Domain situationModel = questionData.situationDomain;
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
            new ArrayList<>()
        );
    }

    @Override
    public DecisionTreeReasonerBackend.Output solve(Input questionData) {
        return null;
    }

    public static abstract class Interface
        extends DomainToBackendInterface<Input, Output, DecisionTreeReasonerBackend> {

        public Interface() {
            super(DecisionTreeReasonerBackend.class);
        }

        private final static String ERROR_NODE_ATTR = "errorNode";
        @Override
        public InterpretSentenceResult interpretJudgeOutput(
            Question judgedQuestion,
            Output backendOutput
        ) {

            if(!backendOutput.isReasoningDone){
                return interpretJudgeNotPerformed(judgedQuestion, backendOutput.situation);
            }

            List<DecisionTreeReasoner.DecisionTreeEvaluationResult> errResults =
                backendOutput.results.stream()
                    .filter(result -> result.getNode().getMetadata().get(ERROR_NODE_ATTR) != null)
                    .toList();
            List<ViolationEntity> mistakes = errResults.stream()
                .map(result -> {
                    String errorName = result.getNode().getMetadata().get(ERROR_NODE_ATTR).toString();
                    ViolationEntity violation = new ViolationEntity();
                    violation.setLawName(errorName);
                    violation.setViolationFacts(new ArrayList<>());
                    return violation;
                })
                .collect(Collectors.toList());

            InterpretSentenceResult result = new InterpretSentenceResult();
            result.violations = mistakes;
            result.correctlyAppliedLaws = new ArrayList<>();
            result.isAnswerCorrect = mistakes.isEmpty();

            updateJudgeInterpretationResult(result, backendOutput);

            result.explanations = makeExplanations(
                errResults,
                backendOutput.situation,
                getUserLanguageByQuestion(judgedQuestion)
            );
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
                return Language.ENGLISH;  // fallback if it cannot be figured out
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

        private List<HyperText> makeExplanations(
            List<DecisionTreeReasoner.DecisionTreeEvaluationResult> results,
            LearningSituation situation,
            Language lang
        ){

            QuestioningSituation textSituation = new QuestioningSituation(
                situation.getDomain(),
                lang.toLocaleString()
            );

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

        @Override
        public void updateQuestionAfterSolve(
            Question question,
            Output backendOutput
        ) {}
    }
}
