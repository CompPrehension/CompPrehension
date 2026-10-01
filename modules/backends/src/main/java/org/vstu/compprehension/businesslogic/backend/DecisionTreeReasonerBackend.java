package org.vstu.compprehension.businesslogic.backend;

import org.vstu.compprehension.businesslogic.domains.DecisionTreeReasoningDomain;
import org.vstu.compprehension.data.question.AnswerHypothesisData;
import org.vstu.compprehension.data.question.HypothesisClarificationData;
import org.vstu.compprehension.data.question.ViolationData;
import io.brookite.termannotations.DomainTermAnnotationProcessor;
import its.model.TypedVariable;
import its.model.definition.DomainModel;
import its.model.nodes.*;
import its.questions.gen.formulations.TemplatingUtils;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.AggregationDecisionTreeTraceElement;
import its.reasoner.nodes.DecisionTreeReasoner;
import its.reasoner.nodes.DecisionTreeTrace;
import its.reasoner.nodes.DecisionTreeTraceElement;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.vstu.compprehension.businesslogic.DomainToBackendAdapter;
import org.vstu.compprehension.businesslogic.Explanation;
import org.vstu.compprehension.data.question.QuestionContentData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.domains.DomainBase;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.businesslogic.HyperText;

import java.util.*;
import java.util.stream.Collectors;

import static org.vstu.compprehension.businesslogic.domains.Domain.InterpretSentenceResult;

/**
 * A reasoning backend that works with decision-tree based reasoning;<br>
 * Uses {@link DomainModel} objects to encode data, and {@link DecisionTree}s to encode reasoning processes.<br>
 * Domains aiming to support this backend should have a corresponding {@link Interface}
 * @see its.model.DomainSolvingModel
 */
@Primary
@Component
// То же, что @RequestScope, но без зависимости бизнес-логики от spring-web:
// @RequestScope — это ровно @Scope("request") с прокси на класс.
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Log4j2
public class DecisionTreeReasonerBackend
    implements Backend<DecisionTreeReasonerBackend.Input, DecisionTreeReasonerBackend.Output>
{
    public static String BACKEND_ID = "DTReasoner";
    public final static int MAX_SIMILAR_EXPLANATION_COUNT = 3;

    private static final Map<String, Map<String, String>> utilLoc = Map.ofEntries(
            Pair.of("RU", Map.ofEntries(
                    Pair.of("andAlsoHint", "влияет всё из нижеперечисленного..."),
                    Pair.of("orAlsoHint", "влияет любое из нижеперечисленного..."),
                    Pair.of("moreErrorHint", "...и еще %d похожих ошибок"),
                    Pair.of("moreHint", "...и еще %d похожих подсказок"),
                    Pair.of("clarificationPrompt", "Почему вы дали такой ответ?")
            )),
            Pair.of("EN", Map.ofEntries(
                    Pair.of("andAlsoHint", "it is influenced by all of the following..."),
                    Pair.of("orAlsoHint", "it is influenced by any of the following..."),
                    Pair.of("moreErrorHint", "...and also %d more similar errors"),
                    Pair.of("moreHint", "...and also %d more similar hints"),
                    Pair.of("clarificationPrompt", "Why did you give this answer?")
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

    /**
     * Собрать все объяснения с учетом агрегаций в древовидную структуру
     * @param type тип объяснения, например объяснение ошибки
     * @param trace трасса путей интерпретатора по Decision Tree
     * @param domainModel домен Decision Tree
     * @param appDomain домен - компонент CompPrehension
     * @param lang язык пользователя
     * @return объект объяснения в виде агрегированных в него других объяснений
     */
    public static Explanation collectExplanationsFromTrace(Explanation.Type type,
                                                           DecisionTreeTrace trace,
                                                           DomainModel domainModel,
                                                           DecisionTreeReasoningDomain appDomain,
                                                           Language lang) {
        DomainTermAnnotationProcessor annotationProcessor = null;
        if (appDomain.getTermDictionary().isPresent()) {
            annotationProcessor = new DomainTermAnnotationProcessor(appDomain.getTermDictionary().get(), lang.toLocale());
        }
        Explanation result = Explanation.aggregate(type, collectExplanations(type, trace, null,
                AggregationPolicy.Default,
                domainModel, annotationProcessor, lang));
        String prefix = Explanation.getCommonPrefix(result.getChildren(), "");
        if (result.getChildren().size() > 1 && !prefix.isEmpty()) {
            result.setRawMessage(new HyperText(prefix.trim().concat(":")));
        }
        // Если в ветви все объяснения принадлежат одному навыку, то у всей ветви этот навык
        if (result.getChildren().stream().map(Explanation::getDomainLawNames).collect(Collectors.toSet()).size() == 1) {
            result.setCurrentDomainLawName(result.getChildren().getFirst().getCurrentDomainLawName());
        }
        reduceSimilarExplanations(result.getChildren(), type, lang);
        return result;
    }

    /**
     * hyp-агрегации, объясняющие ответ студента.
     * Внутрь других агрегаций заходит только по ветвям с их итогом — остальные ответ не объясняют.
     */
    private static @NotNull List<AggregationDecisionTreeTraceElement<?>> findHypothesisAggregations(
            @NotNull DecisionTreeTrace trace) {
        List<AggregationDecisionTreeTraceElement<?>> found = new ArrayList<>();
        findHypothesisAggregations(trace, found);
        return found;
    }

    private static void findHypothesisAggregations(@NotNull DecisionTreeTrace trace,
                                                   @NotNull List<AggregationDecisionTreeTraceElement<?>> found) {
        for (DecisionTreeTraceElement<?, ?> element : trace) {
            if (element instanceof AggregationDecisionTreeTraceElement<?> aggregation && isHypothesisAggregation(aggregation)) {
                found.add(aggregation);
                continue;
            }
            for (DecisionTreeTrace subTrace : explainedNestedTraces(element)) {
                findHypothesisAggregations(subTrace, found);
            }
        }
    }

    /** Гипотезы о рассуждении студента: ветви hyp-агрегаций, объяснившие ответ. */
    private static @NotNull List<AnswerHypothesisData> collectHypotheses(
            @NotNull List<AggregationDecisionTreeTraceElement<?>> hypothesisAggregations) {
        return hypothesisAggregations.stream()
                .flatMap(aggregation -> explainingHypothesisBranches(aggregation).stream())
                .map(branch -> new AnswerHypothesisData(
                        requireBranchMeta(branch, "hypothesis"),
                        branch.getBranchResult() == BranchResult.CORRECT))
                .toList();
    }

    // Ошибку объясняют несколько гипотез: каким из заблуждений рассуждал студент, неизвестно.
    private static boolean isAmbiguousError(@NotNull AggregationDecisionTreeTraceElement<?> aggregation) {
        return aggregation.getNodeResult() == BranchResult.ERROR
                && explainingHypothesisBranches(aggregation).size() > 1;
    }

    /** Варианты уточняющего вопроса: причина ответа по каждой гипотезе и объяснение этого заблуждения. */
    private static @NotNull List<HypothesisClarificationData.Option> collectClarificationOptions(
            @NotNull AggregationDecisionTreeTraceElement<?> aggregation,
            @NotNull DomainModel domainModel,
            @NotNull String localizationCode) {
        return explainingHypothesisBranches(aggregation).stream()
                .map(branch -> {
                    var situation = new LearningSituation(domainModel, branch.getResultingElement().getVariablesSnapshot());
                    var conclusion = (BranchResultNode) branch.getResultingElement().getNode();
                    var reason = conclusion.getMetadata().get(localizationCode, "reason");
                    if (reason == null) {
                        throw new IllegalStateException("Hypothesis branch concluded by " + conclusion.getDescription()
                                + " has no " + localizationCode + " 'reason' metadata");
                    }
                    return new HypothesisClarificationData.Option(
                            requireBranchMeta(branch, "hypothesis"),
                            TemplatingUtils.interpret(reason.toString(), situation, localizationCode, Map.of()),
                            Interface.extractExplanation(conclusion, localizationCode, situation).getRawMessage().getText());
                })
                .toList();
    }

    private static boolean isHypothesisAggregation(@NotNull AggregationDecisionTreeTraceElement<?> aggregation) {
        return aggregation.getNode().getAggregationMethod() == AggregationMethod.HYP;
    }

    private static @NotNull List<DecisionTreeTrace> explainingHypothesisBranches(
            @NotNull AggregationDecisionTreeTraceElement<?> aggregation) {
        return aggregation.nestedTraces().stream()
                .filter(branch -> branch.getBranchResult() != BranchResult.NULL)
                .toList();
    }

    private static @NotNull String requireBranchMeta(@NotNull DecisionTreeTrace branch, @NotNull String key) {
        var node = branch.getResultingElement().getNode();
        var value = node.getMetadata().getString(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Hypothesis branch concluded by " + node.getDescription()
                    + " has no '" + key + "' metadata");
        }
        return value;
    }

    /**
     * Ошибку объясняют несколько гипотез, а узел hyp задаёт общее объяснение: показывается только оно,
     * потому что неизвестно, каким из заблуждений рассуждал студент.
     */
    private static boolean hasAmbiguousErrorExplanation(@NotNull DecisionTreeTraceElement<?, ?> element) {
        return element instanceof AggregationDecisionTreeTraceElement<?> aggregation
                && isHypothesisAggregation(aggregation)
                && isAmbiguousError(aggregation)
                && aggregation.getNode().getMetadata().containsAny("explanation");
    }

    // Все ветви hyp висят под одной развилкой, поэтому навык общего объяснения — их общий навык.
    private static @NotNull Explanation extractAmbiguousErrorExplanation(@NotNull AggregationDecisionTreeTraceElement<?> aggregation,
                                                                         @NotNull String localizationCode,
                                                                         @NotNull LearningSituation learningSituation) {
        var skills = explainingHypothesisBranches(aggregation).stream()
                .map(branch -> requireBranchMeta(branch, "skill"))
                .collect(Collectors.toSet());
        if (skills.size() != 1) {
            throw new IllegalStateException("Hypotheses of " + aggregation.getNode().getDescription()
                    + " belong to different skills " + skills);
        }
        var explanation = new Explanation(Explanation.Type.ERROR, interpretExplanationTemplate(
                aggregation.getNode(), Explanation.Type.ERROR, localizationCode, learningSituation));
        explanation.setCurrentDomainLawName(skills.iterator().next());
        return explanation;
    }

    // Агрегацию объясняют только ветви с её же итогом: при верном итоге or/hyp ошибки неподошедших ветвей
    // не показываются, при ошибочном итоге and — подсказки удачных ветвей.
    private static Collection<DecisionTreeTrace> explainedNestedTraces(DecisionTreeTraceElement<?, ?> element) {
        if (element instanceof AggregationDecisionTreeTraceElement<?> aggregation) {
            return aggregation.nestedTraces().stream()
                    .filter(branch -> branch.getBranchResult() == aggregation.getNodeResult())
                    .toList();
        }
        return Objects.requireNonNullElse(element.nestedTraces(), List.of());
    }

    // Рекурсивный сбор объяснений для очередной трассы дерева
    private static List<Explanation> collectExplanations(Explanation.Type type,
                                                         DecisionTreeTrace trace,
                                                         Explanation parent,
                                                         AggregationPolicy policy,
                                                         DomainModel domain,
                                                         DomainTermAnnotationProcessor annotationProcessor,
                                                         Language lang) {
        List<Explanation> traceExplanations = new ArrayList<>(); // временный буфер
        for (DecisionTreeTraceElement<?, ?> element : trace) {
            LearningSituation learningSituation = new LearningSituation(domain, element.getVariablesSnapshot());
            Explanation explanation = null;
            if (Objects.requireNonNullElse(element.nestedTraces(), new ArrayList<DecisionTreeTrace>()).isEmpty()
                    && element.getNode() instanceof BranchResultNode res
                    && (type == Explanation.Type.ERROR) != element.getNodeResult().equals(BranchResult.CORRECT)
                    && element.getNode().getMetadata().containsAny("explanation")) {
                // одиночное объяснение по заданному типу объяснения
                explanation = Interface.extractExplanation(res,
                        lang.toLocaleString(), learningSituation);
            } else if (type == Explanation.Type.ERROR && hasAmbiguousErrorExplanation(element)) {
                explanation = extractAmbiguousErrorExplanation((AggregationDecisionTreeTraceElement<?>) element,
                        lang.toLocaleString(), learningSituation);
            }
            if (explanation != null) {
                if (annotationProcessor != null) {
                    var annotatedMessage = annotationProcessor.apply(explanation.getRawMessage().toString(), new DomainTermTooltipVisualizer());
                    explanation.setRawMessage(new HyperText(annotatedMessage));
                }
                traceExplanations.add(explanation);
            } else {
                // Элемент трассы может включать другие трассы
                Explanation newParent = parent;
                AggregationPolicy newPolicy = policy;
                if (element.getNode() instanceof AggregationNode agg && agg.getAggregationMethod().equals(AggregationMethod.AND)) {
                    newPolicy = AggregationPolicy.SimAND;
                    if (type == Explanation.Type.HINT && policy != newPolicy) {
                        // Для Sim:AND и подсказок элементы агрегаций должны быть объединены, если только он не находится в агрегации AND уже
                        newParent = new Explanation(type, ":");
                        traceExplanations.add(newParent);
                    }
                } else if (element.getNode() instanceof AggregationNode agg && agg.getAggregationMethod().equals(AggregationMethod.OR)) {
                    newPolicy = AggregationPolicy.SimOR;
                    if (type == Explanation.Type.ERROR && policy != newPolicy) {
                        // Для Sim:OR и ошибок элементы агрегаций должны быть объединены в новую ветвь, если только он не находится в агрегации OR уже
                        String msg = String.format("<i>%s</i>", utilLoc.get(lang.toLocaleString()).get("orAlsoHint"));
                        newParent = new Explanation(type, msg);
                        traceExplanations.add(newParent);
                    }
                }
                // Собрать с дочерних трасс элементы
                for (DecisionTreeTrace subTrace : explainedNestedTraces(element)) {
                    traceExplanations.addAll(collectExplanations(type, subTrace, newParent, newPolicy, domain,
                            annotationProcessor, lang));
                }
                // Если в агрегированной ветви один элемент - хранить в буфере только его, а если вообще нет элементов - удалить ветвь
                if (newParent != null && (newParent.getChildren().isEmpty() || newParent.getChildren().size() == 1)) {
                    traceExplanations.remove(newParent);
                    if (newParent.getChildren().size() == 1) traceExplanations.add(newParent.getChildren().getFirst());
                }
            }
        }

        if (parent == null) {
            ArrayList<Explanation> list = new ArrayList<>(traceExplanations.stream()
                    .filter(e -> !e.isEmpty())
                    .toList());
            reduceSimilarExplanations(list, type, lang);
            return list;
        } else {
            parent.getChildren().addAll(traceExplanations.stream()
                    .filter(e -> !e.isEmpty())
                    .toList());
            reduceSimilarExplanations(parent.getChildren(), type, lang);
            // Если в ветви все объяснения принадлежат одному навыку, то у всей ветви этот навык
            if (parent.getChildren().stream().map(Explanation::getDomainLawNames).collect(Collectors.toSet()).size() == 1) {
                parent.setCurrentDomainLawName(parent.getChildren().getFirst().getCurrentDomainLawName());
            }
            return List.of();
        }
    }

    // Сокращает число схожих объяснений (схожесть по навыкам) в списке/ветви, схожие элементы заменяются подсказкой с числом
    private static void reduceSimilarExplanations(Collection<Explanation> explanations, Explanation.Type type, Language lang) {
        Map<String, Integer> skillCounter = new HashMap<>();
        List<Explanation> deleteCandidates = new ArrayList<>();
        for (Explanation item : explanations) {
            int total = skillCounter.getOrDefault(item.getCurrentDomainLawName(), 0) + 1;
            skillCounter.put(item.getCurrentDomainLawName(), total);
            if (total > MAX_SIMILAR_EXPLANATION_COUNT && item.getCurrentDomainLawName() != null) {
                deleteCandidates.add(item);
            }
        }
        explanations.removeAll(deleteCandidates);
        if (!deleteCandidates.isEmpty()) {
            String skipTemplate = type == Explanation.Type.ERROR ? utilLoc.get(lang.toLocaleString()).get("moreErrorHint") :
                    utilLoc.get(lang.toLocaleString()).get("moreHint");
            skipTemplate = String.format(skipTemplate, deleteCandidates.size());
            explanations.add(new Explanation(type, String.format("<i>%s</i>", skipTemplate)));
        }
    }

    private static String interpretExplanationTemplate(DecisionTreeElement node,
                                                       Explanation.Type type,
                                                       String localizationCode,
                                                       LearningSituation learningSituation) {
        Object explanation = node.getMetadata().get(localizationCode, "explanation");
        String prefix = Interface.getCommonExplanationPrefix(learningSituation, node.getDecisionTree(), type, localizationCode);
        String explanationTemplate = explanation == null ? "WRONG" : prefix.concat(explanation.toString());
        String expanded = TemplatingUtils.interpret(explanationTemplate, learningSituation, localizationCode, Map.of());
        {
            if (expanded.contains("операто ")) {
                // Fix spelling (note the space at the end).
                expanded = expanded.replace("операто ", "оператор ");
            }
            if (expanded.contains("operato ")) {
                // Fix spelling (note the space at the end).
                expanded = expanded.replace("operato ", "operator ");
            }
        }
        return expanded;
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

    public interface Interface extends DomainToBackendAdapter<Input, Output, DecisionTreeReasonerBackend> {

        DecisionTreeReasoningDomain getDomain();

        @Override
        default InterpretSentenceResult interpretJudgeOutput(
            QuestionData judgedQuestion,
            Output backendOutput,
            Language language
        ) {

            if(!backendOutput.isReasoningDone){
                return interpretJudgeNotPerformed(judgedQuestion, backendOutput.situation, language);
            }
            List<DecisionTreeTraceElement<?, ?>> traceElements = nestedTraceElements(backendOutput.results);

            DecisionTreeInterpretSentenceResult result = new DecisionTreeInterpretSentenceResult();
            result.isAnswerCorrect = isCorrectAnswer(backendOutput.results);
            result.decisionTreeTrace = backendOutput.results;
            var hypothesisAggregations = findHypothesisAggregations(backendOutput.results);
            result.hypotheses = collectHypotheses(hypothesisAggregations);
            var ambiguous = hypothesisAggregations.stream()
                    .filter(DecisionTreeReasonerBackend::isAmbiguousError)
                    .toList();
            if (ambiguous.size() > 1) {
                throw new IllegalStateException("An answer is ambiguously explained by several hyp aggregations: "
                        + ambiguous.stream().map(aggregation -> aggregation.getNode().getDescription()).toList());
            }
            if (!ambiguous.isEmpty()) {
                result.clarification = new HypothesisClarificationData(
                        makeClarificationPrompt(judgedQuestion, backendOutput, language),
                        collectClarificationOptions(ambiguous.getFirst(),
                                backendOutput.situation.getDomainModel(), language.toLocaleString()));
            }
            for (DecisionTreeTraceElement<?,?> res : traceElements) {
                String[] resSkill = res.getNode().getMetadata().containsAny("skill") && res.getNode().getMetadata().get("skill") != null ?
                        res.getNode().getMetadata().get("skill").toString().split(";") : new String[0];
                String[] resLaw = res.getNode().getMetadata().containsAny("law") && res.getNode().getMetadata().get("law") != null ?
                        res.getNode().getMetadata().get("law").toString().split(";") : new String[0];
                Collections.addAll(result.domainSkills, resSkill);
                Collections.addAll(result.domainNegativeLaws, resLaw);
            }

            updateJudgeInterpretationResult(result, backendOutput);

            result.explanation = collectExplanationsFromTrace(Explanation.Type.ERROR, backendOutput.results,
                    backendOutput.situation.getDomainModel(),
                    getDomain(), language
            );
            if (!result.isAnswerCorrect) {
                List<ViolationData> mistakes = result.explanation.getDomainLawNames()
                        .stream().map(errorName -> {
                            ViolationData violation = new ViolationData();
                            violation.setLawName(errorName);
                            violation.setViolationFacts(new ArrayList<>());
                            return violation;
                        })
                        .collect(Collectors.toList());
                result.violations = mistakes;
            } else {
                result.violations = List.of();
            }
            result.correctlyAppliedLaws = new ArrayList<>();
            return result;
        }

        /** Вопрос студенту о причине ответа, который объясняют несколько гипотез. */
        default @NotNull String makeClarificationPrompt(@NotNull QuestionData judgedQuestion,
                                                        @NotNull Output backendOutput,
                                                        @NotNull Language language) {
            return utilLoc.get(language.toLocaleString()).get("clarificationPrompt");
        }

        /**
         * Create an interpretation result for a situation, in which a reasoning could not be performed
         * (Currently only possible if not all input variables are present)
         * @param judgedQuestion a question which prompted the unfinished judge
         * @param preparedSituation a learning situation that was prepared for this question by {@link #prepareBackendInfoForJudge} 
         */
        InterpretSentenceResult interpretJudgeNotPerformed(
            QuestionData judgedQuestion,
            LearningSituation preparedSituation,
            Language language
        );

        /**
         * Update a {@link #judge} interpretation result with domain-specific logic
         * Mainly used on {@link InterpretSentenceResult#IterationsLeft}
         * @param interpretationResult the updated result
         * @param backendOutput the output from the backend's {@link #judge} method
         */
        void updateJudgeInterpretationResult(
            InterpretSentenceResult interpretationResult,
            Output backendOutput
        );

        static String getCommonExplanationPrefix(LearningSituation situation,
                                                        DecisionTree dt,
                                                        Explanation.Type type, String localizationCode) {
            Object meta = dt.getMainBranch().getMetadata().get(localizationCode,
                    (type == Explanation.Type.HINT ? "hint" : "error") + "_prefix");
            String rawPrefix = meta != null ? meta.toString() : "";
            String expanded = TemplatingUtils.interpret(rawPrefix, situation, localizationCode, Map.of());
            {
                if (expanded.contains("операто ")) {
                    // Fix spelling (note the space at the end).
                    expanded = expanded.replace("операто ", "оператор ");
                }
                if (expanded.contains("operato ")) {
                    // Fix spelling (note the space at the end).
                    expanded = expanded.replace("operato ", "operator ");
                }
            }
            return expanded;
        }

        static boolean isCorrectAnswer(@NotNull DecisionTreeTrace trace) {
            if (List.of(Boolean.TRUE, true, BranchResult.CORRECT).contains(trace.getBranchResult())) {
                return true;
            }
            return false;
        }

        static Explanation extractExplanation(BranchResultNode resultNode,
                                              String localizationCode,
                                              LearningSituation learningSituation){
            Explanation.Type type = resultNode.getValue() == BranchResult.CORRECT ?
                    Explanation.Type.HINT : Explanation.Type.ERROR;
            Explanation expl = new Explanation(type,
                    interpretExplanationTemplate(resultNode, type, localizationCode, learningSituation));
            if (resultNode.getMetadata().containsAny("skill")) {
                String skillName = resultNode.getMetadata().getString("skill");
                expl.setCurrentDomainLawName(skillName);
            }
            if (resultNode.getMetadata().containsAny("muted")
                    && resultNode.getMetadata().get("muted").toString().toLowerCase().trim().equals("true")) {
                expl.setMuted(true);
            }
            return expl;
        }

        @Override
        default QuestionContentData updateQuestionAfterSolve(
            QuestionContentData question,
            Output backendOutput
        ) {
            return question;
        }
    }
}
