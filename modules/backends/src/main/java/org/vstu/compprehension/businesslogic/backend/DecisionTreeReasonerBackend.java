package org.vstu.compprehension.businesslogic.backend;

import org.vstu.compprehension.businesslogic.domains.DecisionTreeReasoningDomain;
import org.vstu.compprehension.businesslogic.domains.Judgement;
import org.vstu.compprehension.businesslogic.domains.Reasoning;
import org.vstu.compprehension.businesslogic.domains.ReasoningInquiry;
import org.vstu.compprehension.data.question.ViolationData;
import io.brookite.termannotations.DomainTermAnnotationProcessor;
import its.model.DomainSolvingModel;
import its.model.TypedVariable;
import its.model.definition.DomainModel;
import its.model.nodes.*;
import its.questions.gen.formulations.TemplatingUtils;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.AggregationDecisionTreeTraceElement;
import its.reasoner.nodes.DecisionTreeReasoner;
import its.reasoner.nodes.DecisionTreeTrace;
import its.reasoner.nodes.DecisionTreeTraceElement;
import its.reasoner.nodes.RedirectedBranchResultDecisionTreeTraceElement;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import java.util.stream.Stream;

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
                    Pair.of("besides", "Кроме того, ")
            )),
            Pair.of("EN", Map.ofEntries(
                    Pair.of("andAlsoHint", "it is influenced by all of the following..."),
                    Pair.of("orAlsoHint", "it is influenced by any of the following..."),
                    Pair.of("moreErrorHint", "...and also %d more similar errors"),
                    Pair.of("moreHint", "...and also %d more similar hints"),
                    Pair.of("besides", "Besides, ")
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
     * @param solvingModel all decision trees of the domain, if the tree calls other trees by name
     */
    public record Input(
        DomainModel situationDomainModel,
        DecisionTree decisionTree,
        @Nullable DomainSolvingModel solvingModel
    ){
        public Input(DomainModel situationDomainModel, DecisionTree decisionTree) {
            this(situationDomainModel, decisionTree, null);
        }
    }

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
     * Рассуждения студента, которыми дерево объяснило ответ: шаги каждого — выводы с гипотезой, от внешнего
     * к вложенному. Рассуждение ветвится на hyp по ветвям с выводом не null: это разные способы рассуждать.
     * В остальные агрегации заходит только по ветвям с их итогом — прочие ответ не объясняют. Пусто — гипотез нет.
     */
    public static @NotNull List<List<DecisionTreeTraceElement<?, ?>>> collectHypothesisPaths(@NotNull DecisionTreeTrace trace) {
        return collectPaths(trace).stream().filter(path -> !path.isEmpty()).toList();
    }

    private static @NotNull List<List<DecisionTreeTraceElement<?, ?>>> collectPaths(@NotNull DecisionTreeTrace trace) {
        List<List<DecisionTreeTraceElement<?, ?>>> paths = List.of(List.of());
        for (DecisionTreeTraceElement<?, ?> element : trace) {
            boolean isHypothesis = element instanceof AggregationDecisionTreeTraceElement<?> aggregation
                    && isHypothesisAggregation(aggregation);
            var branches = isHypothesis
                    ? explainingHypothesisBranches((AggregationDecisionTreeTraceElement<?>) element)
                    : explainedNestedTraces(element);
            if (branches.isEmpty()) {
                continue;
            }
            var alternatives = new ArrayList<List<DecisionTreeTraceElement<?, ?>>>();
            for (DecisionTreeTrace branch : branches) {
                var branchPaths = collectPaths(branch);
                if (isHypothesis && branchPaths.stream().anyMatch(List::isEmpty)) {
                    throw new IllegalStateException("Hypothesis branch concluded by "
                            + branch.getResultingElement().getNode().getDescription() + " has no 'hypothesis' metadata");
                }
                alternatives.addAll(branchPaths);
            }
            paths = paths.stream()
                    .flatMap(path -> alternatives.stream()
                            .map(alternative -> Stream.concat(path.stream(), alternative.stream()).toList()))
                    .toList();
        }
        // Вывод, к которому пришла трасса, — внешнее допущение по отношению к рассуждениям внутри неё. Вывод через
        // вызов другого дерева — это вывод того дерева, он уже есть во вложенной трассе.
        var last = trace.getLast();
        if (!(last instanceof RedirectedBranchResultDecisionTreeTraceElement) && last.getNode() instanceof BranchResultNode
                && last.getNodeResult() != BranchResult.NULL && last.getNode().getMetadata().getString("hypothesis") != null) {
            paths = paths.stream()
                    .map(path -> Stream.concat(Stream.<DecisionTreeTraceElement<?, ?>>of(last), path.stream()).toList())
                    .toList();
        }
        return paths;
    }

    /** Рассуждение по всей трассе, ход мысли в котором не установлен. */
    private static @NotNull Reasoning makeUnexplainedReasoning(@NotNull DecisionTreeTrace trace,
                                                               boolean isAnswerCorrect,
                                                               @NotNull DomainModel domainModel,
                                                               @NotNull DecisionTreeReasoningDomain appDomain,
                                                               @NotNull Language lang) {
        var explanation = collectExplanationsFromTrace(Explanation.Type.ERROR, trace, domainModel, appDomain, lang);
        var violations = isAnswerCorrect ? List.<ViolationData>of()
                : explanation.getDomainLawNames().stream().map(DecisionTreeReasonerBackend::makeViolation).toList();
        return new Reasoning(List.of(), isAnswerCorrect, null, explanation, violations, collectAppliedLaws(trace));
    }

    /** Рассуждения, которыми студент мог прийти к ответу: по одному на каждый путь, объяснивший ответ. */
    private static @NotNull List<Reasoning> makeReasonings(@NotNull List<List<DecisionTreeTraceElement<?, ?>>> paths,
                                                           boolean isAnswerCorrect,
                                                           @NotNull DomainModel domainModel,
                                                           @NotNull DecisionTreeReasoningDomain appDomain,
                                                           @NotNull Language lang) {
        var reasonings = new ArrayList<Reasoning>();
        for (var path : paths) {
            addReasoning(reasonings, makeReasoning(path, isAnswerCorrect, domainModel, appDomain, lang));
        }
        return reasonings;
    }

    // Одно рассуждение может прийти из разных прочтений выражения. Это одно рассуждение, если причина у него та же или
    // у одного из прочтений своей причины нет; законы прочтений объединяются.
    private static void addReasoning(@NotNull List<Reasoning> reasonings, @NotNull Reasoning added) {
        for (int i = 0; i < reasonings.size(); i++) {
            var known = reasonings.get(i);
            if (!known.hypotheses().equals(added.hypotheses())
                    || known.reason() != null && added.reason() != null && !known.reason().equals(added.reason())) {
                continue;
            }
            var kept = known.reason() != null ? known : added;
            reasonings.set(i, new Reasoning(kept.hypotheses(), kept.isCorrect(), kept.reason(), kept.explanation(),
                    union(known.violations(), added.violations()), union(known.appliedLaws(), added.appliedLaws())));
            return;
        }
        reasonings.add(added);
    }

    private static <T> @NotNull List<T> union(@NotNull List<T> left, @NotNull List<T> right) {
        return Stream.concat(left.stream(), right.stream()).distinct().toList();
    }

    // Ошибочное рассуждение студенту называют его ошибочные шаги по порядку, верное — все шаги. Верный шаг без своей
    // причины называется правилом из объяснения; рассуждение без причины на выбор не предлагается.
    private static @NotNull Reasoning makeReasoning(@NotNull List<DecisionTreeTraceElement<?, ?>> path,
                                                    boolean isAnswerCorrect,
                                                    @NotNull DomainModel domainModel,
                                                    @NotNull DecisionTreeReasoningDomain appDomain,
                                                    @NotNull Language lang) {
        var localizationCode = lang.toLocaleString();
        boolean isCorrect = path.stream().allMatch(step -> step.getNodeResult() == BranchResult.CORRECT);
        var told = isCorrect ? path : path.stream().filter(step -> step.getNodeResult() == BranchResult.ERROR).toList();

        var reasons = new ArrayList<String>();
        var explanations = new ArrayList<String>();
        for (var step : told) {
            var conclusion = step.getNode();
            var situation = new LearningSituation(domainModel, step.getVariablesSnapshot());
            var reason = conclusion.getMetadata().get(localizationCode, "reason");
            if (reason == null && isCorrect) {
                reason = conclusion.getMetadata().get(localizationCode, "explanation");
            }
            if (reason == null && !isCorrect) {
                throw new IllegalStateException("Hypothesis branch concluded by " + conclusion.getDescription()
                        + " has no " + localizationCode + " 'reason' metadata");
            }
            if (reason != null) {
                // Причина — отдельная фраза, но может начинаться с подстановки, а названия в модели строчные.
                reasons.add(TemplatingUtils.capitalize(TemplatingUtils.interpret(reason.toString(), situation, localizationCode, Map.of())));
            }
            var explanation = conclusion.getMetadata().get(localizationCode, "explanation");
            if (explanation != null) {
                explanations.add(TemplatingUtils.interpret(explanation.toString(), situation, localizationCode, Map.of()));
            }
        }

        var hypotheses = path.stream().map(step -> step.getNode().getMetadata().getString("hypothesis")).toList();
        var violations = isCorrect ? List.<ViolationData>of()
                : told.stream().map(step -> makeViolation(requireSkill(step))).toList();
        var appliedLaws = path.stream()
                .filter(step -> step.getNodeResult() == BranchResult.CORRECT)
                .flatMap(step -> Arrays.stream(requireSkill(step).split(";")))
                .distinct()
                .sorted()
                .toList();
        return new Reasoning(hypotheses, isCorrect, reasons.isEmpty() ? null : String.join(" ", reasons),
                makeReasoningExplanation(told.getFirst(), explanations, isCorrect, isAnswerCorrect, domainModel, appDomain, lang),
                violations, appliedLaws);
    }

    // Объяснения шагов продолжают рамку дерева первого шага: рамку ошибки, подсказки или, если к верному ответу
    // привело заблуждение, рамку ошибочного рассуждения («не может иметь тип» тогда неверно).
    private static @NotNull Explanation makeReasoningExplanation(@NotNull DecisionTreeTraceElement<?, ?> firstStep,
                                                                 @NotNull List<String> explanations,
                                                                 boolean isCorrect,
                                                                 boolean isAnswerCorrect,
                                                                 @NotNull DomainModel domainModel,
                                                                 @NotNull DecisionTreeReasoningDomain appDomain,
                                                                 @NotNull Language lang) {
        var type = isCorrect ? Explanation.Type.HINT : Explanation.Type.ERROR;
        if (explanations.isEmpty()) {
            return Explanation.empty(type);
        }
        var localizationCode = lang.toLocaleString();
        var situation = new LearningSituation(domainModel, firstStep.getVariablesSnapshot());
        var tree = firstStep.getNode().getDecisionTree();
        String prefix;
        if (isAnswerCorrect && !isCorrect) {
            var misreasoningPrefix = tree.getMainBranch().getMetadata().get(localizationCode, "misreasoning_prefix");
            if (misreasoningPrefix == null) {
                throw new IllegalStateException("Decision tree with hypotheses has no " + localizationCode
                        + " 'misreasoning_prefix' metadata");
            }
            prefix = TemplatingUtils.interpret(misreasoningPrefix.toString(), situation, localizationCode, Map.of());
        } else {
            prefix = Interface.getCommonExplanationPrefix(situation, tree, type, localizationCode);
        }
        var explanation = new Explanation(type,
                prefix + String.join(" " + utilLoc.get(localizationCode).get("besides"), explanations));
        explanation.setCurrentDomainLawName(firstStep.getNode().getMetadata().getString("skill"));
        return annotateTerms(explanation, appDomain, lang);
    }

    private static @NotNull String requireSkill(@NotNull DecisionTreeTraceElement<?, ?> step) {
        var skill = step.getNode().getMetadata().getString("skill");
        if (skill == null || skill.isBlank()) {
            throw new IllegalStateException("Hypothesis branch concluded by " + step.getNode().getDescription()
                    + " has no 'skill' metadata");
        }
        return skill;
    }

    private static @NotNull ViolationData makeViolation(@NotNull String lawName) {
        var violation = new ViolationData();
        violation.setLawName(lawName);
        violation.setViolationFacts(new ArrayList<>());
        return violation;
    }

    private static @NotNull List<String> collectAppliedLaws(@NotNull DecisionTreeTrace trace) {
        return LeafEngagedSkillsExtractor.extract(trace).getCorrectlyApplied().stream().sorted().toList();
    }

    private static @NotNull Explanation annotateTerms(@NotNull Explanation explanation,
                                                      @NotNull DecisionTreeReasoningDomain appDomain,
                                                      @NotNull Language lang) {
        if (appDomain.getTermDictionary().isPresent()) {
            explanation.setRawMessage(new HyperText(new DomainTermAnnotationProcessor(appDomain.getTermDictionary().get(),
                    lang.toLocale()).apply(explanation.getRawMessage().toString(), new DomainTermTooltipVisualizer())));
        }
        return explanation;
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

    private static @NotNull ReasoningInquiry makeInquiry(@NotNull DecisionTree tree,
                                                         @NotNull LearningSituation situation,
                                                         boolean isAnswerCorrect,
                                                         @NotNull DecisionTreeReasoningDomain appDomain,
                                                         @NotNull Language lang) {
        var localizationCode = lang.toLocaleString();
        var statement = isAnswerCorrect ? Explanation.empty(Explanation.Type.ERROR)
                : annotateTerms(new Explanation(Explanation.Type.ERROR,
                        interpretTreeMeta(tree, "error_statement", situation, localizationCode)), appDomain, lang);
        return new ReasoningInquiry(interpretTreeMeta(tree, "clarification_prompt", situation, localizationCode), statement);
    }

    private static @NotNull String interpretTreeMeta(@NotNull DecisionTree tree,
                                                     @NotNull String key,
                                                     @NotNull LearningSituation situation,
                                                     @NotNull String localizationCode) {
        var template = tree.getMainBranch().getMetadata().get(localizationCode, key);
        if (template == null) {
            throw new IllegalStateException("Decision tree with hypotheses has no " + localizationCode
                    + " '" + key + "' metadata");
        }
        return TemplatingUtils.interpret(template.toString(), situation, localizationCode, Map.of());
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

    private static @NotNull LearningSituation makeSituation(@NotNull Input input) {
        return new LearningSituation(
            input.situationDomainModel,
            LearningSituation.collectDecisionTreeVariables(input.situationDomainModel),
            input.solvingModel
        );
    }

    @Override
    public DecisionTreeReasonerBackend.Output judge(Input questionData) {
        DecisionTree decisionTree = questionData.decisionTree;
        LearningSituation situation = makeSituation(questionData);

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
        default @NotNull Judgement interpretJudgeOutput(
            QuestionData judgedQuestion,
            Output backendOutput,
            Language language
        ) {
            if (!backendOutput.isReasoningDone) {
                return interpretJudgeNotPerformed(judgedQuestion, backendOutput.situation, language);
            }
            var trace = backendOutput.results;
            var domainModel = backendOutput.situation.getDomainModel();
            boolean isAnswerCorrect = isCorrectAnswer(trace);
            int stepsLeft = countStepsLeft(judgedQuestion, backendOutput, isAnswerCorrect);
            var paths = collectHypothesisPaths(trace);
            if (!paths.isEmpty()) {
                return new Judgement(makeReasonings(paths, isAnswerCorrect, domainModel, getDomain(), language), stepsLeft,
                        makeInquiry(trace.getResultingElement().getNode().getDecisionTree(), backendOutput.situation,
                                isAnswerCorrect, getDomain(), language));
            }
            var reasoning = makeUnexplainedReasoning(trace, isAnswerCorrect, domainModel, getDomain(), language);
            // Ответ, после которого шагов не осталось, завершает задачу: ошибок в нём быть не может.
            if (stepsLeft == 0) {
                reasoning = new Reasoning(List.of(), true, null, Explanation.empty(Explanation.Type.HINT), List.of(),
                        reasoning.appliedLaws());
            }
            return new Judgement(reasoning, stepsLeft);
        }

        /**
         * Create an interpretation result for a situation, in which a reasoning could not be performed
         * (Currently only possible if not all input variables are present)
         * @param judgedQuestion a question which prompted the unfinished judge
         * @param preparedSituation a learning situation that was prepared for this question by {@link #prepareBackendInfoForJudge} 
         */
        Judgement interpretJudgeNotPerformed(
            QuestionData judgedQuestion,
            LearningSituation preparedSituation,
            Language language
        );

        /** Сколько шагов решения осталось после ответа. */
        int countStepsLeft(@NotNull QuestionData judgedQuestion, @NotNull Output backendOutput, boolean isAnswerCorrect);

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
