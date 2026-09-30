package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import its.model.definition.loqi.DomainLoqiBuilder;
import its.model.nodes.AggregationMethod;
import its.model.nodes.BranchResult;
import its.model.nodes.DecisionTree;
import its.reasoner.LearningSituation;
import its.reasoner.nodes.AggregationDecisionTreeTraceElement;
import its.reasoner.nodes.DecisionTreeReasoner;
import its.reasoner.nodes.DecisionTreeTrace;
import its.reasoner.nodes.DecisionTreeTraceElement;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.domains.helpers.DomainSolvingModelLoader;

import java.io.StringReader;
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
            """;

    static final String RULE = "rule";
    static final String OPERAND_TYPE = "operand_type";

    /** Вердикт дерева: итог, виды гипотез с выводом не null и навыки, к которым отнесены выводы. */
    record Verdict(@NotNull BranchResult result, @NotNull Set<String> hypotheses, @NotNull Set<String> skills) {
    }

    private TypeEvaluationTreeFixture() {
    }

    static @NotNull DecisionTree tree() {
        return MODEL.getDecisionTree();
    }

    static @NotNull Verdict judge(@NotNull String operation, @NotNull String leftType,
                                  @NotNull String rightType, @NotNull String answerType) {
        return judgeSituation(TYPES + """
                obj a : Variable { hasType(%s); }
                obj b : Variable { hasType(%s); }
                var E = obj op : %s { hasOperand<OperandPlacement:left>(a); hasOperand<OperandPlacement:right>(b); }
                var T = %s
                """.formatted(leftType, rightType, operation, answerType));
    }

    static @NotNull Verdict judgeSituation(@NotNull String situationLoqi) {
        DomainModel model = MODEL.getMergedTagDomain("python");
        try (var reader = new StringReader(situationLoqi)) {
            model.addMerge(DomainLoqiBuilder.buildDomain(reader));
        }
        model.validateAndThrow();
        var variables = model.getVariables().stream().collect(Collectors.toMap(
                variable -> variable.getName(),
                variable -> variable.getValueObject().getReference()));
        DecisionTreeTrace trace = DecisionTreeReasoner.solve(MODEL.getDecisionTree(), new LearningSituation(model, variables));

        var hypotheses = new TreeSet<String>();
        var skills = new TreeSet<String>();
        for (DecisionTreeTraceElement<?, ?> element : trace) {
            if (element instanceof AggregationDecisionTreeTraceElement<?> aggregation
                    && aggregation.getNode().getAggregationMethod() == AggregationMethod.HYP) {
                for (DecisionTreeTrace branch : aggregation.getBranchTraceMap().values()) {
                    if (branch.getBranchResult() != BranchResult.NULL) {
                        hypotheses.add(branch.getResultingNode().getMetadata().getString("hypothesis"));
                        skills.add(branch.getResultingNode().getMetadata().getString("skill"));
                    }
                }
            }
        }
        // Если ни одна гипотеза не объяснила ответ, навык несёт запасной вывод после hyp.
        var fallbackSkill = trace.getResultingNode().getMetadata().getString("skill");
        if (fallbackSkill != null) {
            skills.add(fallbackSkill);
        }
        return new Verdict(trace.getBranchResult(), hypotheses, skills);
    }
}
