package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import its.model.definition.loqi.DomainLoqiBuilder;
import its.model.expressions.literals.StringLiteral;
import its.model.nodes.AggregationMethod;
import its.model.nodes.BranchResult;
import its.model.nodes.BranchResultNode;
import its.model.nodes.BranchResultRedirectingNode;
import its.model.nodes.DecisionTree;
import its.questions.gen.formulations.TemplatingUtils;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.AggregationDecisionTreeTraceElement;
import its.reasoner.nodes.DecisionTreeReasoner;
import its.reasoner.nodes.DecisionTreeTrace;
import its.reasoner.nodes.DecisionTreeTraceElement;
import its.reasoner.nodes.RedirectedBranchResultDecisionTreeTraceElement;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.businesslogic.domains.helpers.DomainSolvingModelLoader;

import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

final class TypeEvaluationTreeFixture {

    private static final DomainSolvingModel MODEL = DomainSolvingModelLoader.loadFromClasspath(
            TypeEvaluationTreeFixture.class.getClassLoader(),
            "domains/type-evaluation-domain-model/",
            DomainSolvingModel.BuildMethod.LOQI).validate(false);

    static final String TYPES = """
            obj t_int : py_int {}
            obj t_float : py_float {}
            obj t_bool : py_bool {}
            obj t_str : py_str { elementType(t_str); }
            obj t_list_int : py_list { elementType(t_int); }
            obj t_list_list_int : py_list { elementType(t_list_int); }
            obj t_tuple_str_int : py_tuple { itemType<0>(t_str); itemType<1>(t_int); }
            obj t_dict_str_int : py_dict { keyType(t_str); valueType(t_int); }
            obj t_error : py_TypeError {}
            obj t_attribute_error : py_AttributeError {}
            """;

    private static final List<String> LANGUAGES = List.of("RU", "EN");

    static final String RULE = "rule";
    static final String OPERAND_TYPE = "operand_type";
    static final String INAPPLICABLE_ASSUMED = "inapplicable_assumed";

    /**
     * Вердикт дерева: итог, виды гипотез с выводом не null, рассуждения (гипотезы каждого пути, которым дерево
     * объяснило ответ) и навыки, к которым отнесены выводы.
     */
    record Verdict(@NotNull BranchResult result, @NotNull Set<String> hypotheses, @NotNull Set<Set<String>> reasonings,
                   @NotNull Set<String> skills) {
    }

    private TypeEvaluationTreeFixture() {
    }

    static @NotNull DecisionTree tree() {
        return MODEL.getDecisionTree();
    }

    /** Основное дерево и деревья, которые оно вызывает. */
    static @NotNull Collection<DecisionTree> trees() {
        return MODEL.getDecisionTrees().values();
    }

    /** Дерево, которое вызывает вывод через subcall: первый аргумент вызова — имя дерева. */
    static @NotNull DecisionTree calledTree(@NotNull BranchResultRedirectingNode call) {
        var name = ((StringLiteral) call.getCall().getArguments().getFirst()).getValue();
        return Objects.requireNonNull(MODEL.getDecisionTrees().get(name), name);
    }

    /**
     * Операция op с операндами a и b. Прочтения операции (см. {@link #similarOperator}, {@link #foreignVariant}) кладёт
     * в ситуацию генератор — те, что студент может спутать; без них дерево операцию не перечитывает.
     */
    static @NotNull Verdict judge(@NotNull String operation, @NotNull String leftType,
                                  @NotNull String rightType, @NotNull String answerType, @NotNull String... readings) {
        return judgeSituation(TYPES + """
                obj a : Variable { hasType(%s); }
                obj b : Variable { hasType(%s); }
                var E = obj op : %s { hasOperand<OperandPlacement:left>(a); hasOperand<OperandPlacement:right>(b); }
                var T = %s
                """.formatted(leftType, rightType, operation, answerType) + String.join("", readings));
    }

    /** Прочтение операции op с операндами a и b как похожего оператора. */
    static @NotNull String similarOperator(@NotNull String operationClass) {
        return reading(operationClass, "similarOperatorOf");
    }

    /** Прочтение операции op с операндами a и b по правилам другого языка. */
    static @NotNull String foreignVariant(@NotNull String operationClass) {
        return reading(operationClass, "foreignVariantOf");
    }

    private static @NotNull String reading(@NotNull String operationClass, @NotNull String relation) {
        return "obj op_as_%s : %s { hasOperand<OperandPlacement:left>(a); hasOperand<OperandPlacement:right>(b); %s(op); }\n"
                .formatted(operationClass, operationClass, relation);
    }

    static @NotNull Verdict judgeSituation(@NotNull String situationLoqi) {
        DomainModel model = MODEL.getMergedTagDomain("python");
        try (var reader = new StringReader(situationLoqi)) {
            model.addMerge(DomainLoqiBuilder.buildDomain(reader));
        }
        model.validateAndThrow();
        nameUnnamedObjects(model);
        var variables = model.getVariables().stream().collect(Collectors.toMap(
                variable -> variable.getName(),
                variable -> variable.getValueObject().getReference()));
        DecisionTreeTrace trace = DecisionTreeReasoner.solve(MODEL.getDecisionTree(),
                new LearningSituation(model, variables, MODEL));
        renderExplanations(model, trace);

        var paths = DecisionTreeReasonerBackend.collectHypothesisPaths(trace);
        var hypotheses = new TreeSet<String>();
        var skills = new TreeSet<String>();
        for (var step : paths.stream().flatMap(List::stream).toList()) {
            if (DecisionTreeReasonerBackend.isAssumption(step)) {
                hypotheses.add(step.getNode().getMetadata().getString("hypothesis"));
            }
            skills.add(step.getNode().getMetadata().getString("skill"));
        }
        // Ответ, который не объяснил ни один способ рассуждения, относится к навыку запасного вывода правила.
        if (paths.isEmpty()) {
            collectUnexplainedSkills(trace, skills);
        }
        var reasonings = paths.stream()
                .map(path -> path.stream()
                        .filter(DecisionTreeReasonerBackend::isAssumption)
                        .map(step -> step.getNode().getMetadata().getString("hypothesis"))
                        .collect(Collectors.toSet()))
                .collect(Collectors.toSet());
        return new Verdict(trace.getBranchResult(), hypotheses, reasonings, skills);
    }

    // Запасные выводы and без вывода с ошибочной частью не в счёт: это чужое допущение, к ответу не ведущее.
    private static void collectUnexplainedSkills(@NotNull DecisionTreeTrace trace, @NotNull Set<String> skills) {
        for (DecisionTreeTraceElement<?, ?> element : trace) {
            if (element.getNode() instanceof BranchResultNode conclusion && conclusion.getValue() == BranchResult.NULL
                    && conclusion.getMetadata().getString("skill") != null) {
                skills.add(conclusion.getMetadata().getString("skill"));
            }
            if (element instanceof AggregationDecisionTreeTraceElement<?> aggregation
                    && aggregation.getNode().getAggregationMethod() == AggregationMethod.AND
                    && aggregation.getNodeResult() == BranchResult.NULL
                    && aggregation.nestedTraces().stream().anyMatch(branch -> branch.getBranchResult() == BranchResult.ERROR)) {
                continue;
            }
            for (DecisionTreeTrace nested : Objects.requireNonNullElse(element.nestedTraces(), List.<DecisionTreeTrace>of())) {
                collectUnexplainedSkills(nested, skills);
            }
        }
    }

    // Домен называет объекты текстом вопроса; здесь вопроса нет, поэтому имя объекта в модели.
    private static void nameUnnamedObjects(@NotNull DomainModel model) {
        for (var object : model.getObjects()) {
            for (var language : LANGUAGES) {
                if (object.getMetadata().get(language, "localizedName") == null) {
                    object.getMetadata().add(language, "localizedName", "<code>" + object.getName() + "</code>");
                }
            }
        }
    }

    // Шаблон объяснения с ошибкой иначе всплыл бы только у студента: рендерим всё, что трасса может показать.
    private static void renderExplanations(@NotNull DomainModel model, @NotNull DecisionTreeTrace trace) {
        var start = new LearningSituation(model, trace.getFirst().getVariablesSnapshot());
        for (var element : DecisionTreeReasonerBackend.nestedTraceElements(trace)) {
            // Вывод через вызов дерева указывает на конечный узел вызванного дерева, но с переменными вызывающего:
            // этот узел объясняется во вложенной трассе, как и в бэкенде.
            if (element instanceof RedirectedBranchResultDecisionTreeTraceElement) {
                continue;
            }
            var situation = new LearningSituation(model, element.getVariablesSnapshot());
            for (var language : LANGUAGES) {
                for (var key : List.of("explanation", "reason")) {
                    var template = element.getNode().getMetadata().get(language, key);
                    if (template != null) {
                        TemplatingUtils.interpret(template.toString(), situation, language, Map.of());
                    }
                }
            }
        }
        for (var tree : MODEL.getDecisionTrees().values()) {
            for (var language : LANGUAGES) {
                for (var key : List.of("error_prefix", "hint_prefix", "error_statement", "misreasoning_prefix")) {
                    var prefix = tree.getMainBranch().getMetadata().get(language, key);
                    TemplatingUtils.interpret(prefix.toString(), start, language, Map.of());
                }
            }
        }
    }
}
