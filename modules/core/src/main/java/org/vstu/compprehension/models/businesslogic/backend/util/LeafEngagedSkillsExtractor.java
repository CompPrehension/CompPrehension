package org.vstu.compprehension.models.businesslogic.backend.util;

import its.model.nodes.BranchResult;
import its.model.nodes.DecisionTreeElement;
import its.reasoner.nodes.AggregationDecisionTreeTraceElement;
import its.reasoner.nodes.DecisionTreeTrace;
import its.reasoner.nodes.DecisionTreeTraceElement;
import its.reasoner.nodes.WhileCycleDecisionTreeTraceElement;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public final class LeafEngagedSkillsExtractor {

    private LeafEngagedSkillsExtractor() {}

    /**
     * Извлекает из трассы все конечные задействованные навыки по каждой ветке трассы,
     * с учетом агрегаций, циклов, структуры трассы и т.п.
     * @param trace Трасса выполнения DecisionTree
     * @return Наборы примененных и нарушенных конечных навыков
     */
    public static LeafEngagedSkills extract(DecisionTreeTrace trace) {
        return new Impl().extract(trace);
    }

    @Getter @Setter
    public static final class LeafEngagedSkills {

        private final BranchResult branchResult;
        private final Set<String> correctlyApplied = new HashSet<>();
        private final Set<String> violated = new HashSet<>();

        public LeafEngagedSkills(
                BranchResult branchResult,
                Collection<String> correctlyApplied,
                Collection<String> violated
        ) {
            this.branchResult = branchResult;
            if (correctlyApplied != null) this.correctlyApplied.addAll(correctlyApplied);
            if (violated != null) this.violated.addAll(violated);
        }

        public void add(LeafEngagedSkills part) {
            correctlyApplied.addAll(part.correctlyApplied);
            violated.addAll(part.violated);
        }
    }

    private static final class Impl {

        private LeafEngagedSkills extract(DecisionTreeTrace trace) {

            // Получаем навыки из конечного узла этой ветки
            LeafEngagedSkills result = extractLeafSkills(trace);

            // Проходим по динамическим элементам и агрегируем вложенные результаты
            for (DecisionTreeTraceElement<?, ?> elem : trace) {
                if (elem instanceof AggregationDecisionTreeTraceElement<?> agg) {
                    result.add(processAggregation(agg));
                } else if (elem instanceof WhileCycleDecisionTreeTraceElement cycle) {
                    result.add(processCycle(cycle));
                }
                // Link/BranchResult здесь не рассматриваем, так как
                // BranchResult обрабатывается в extractLeafSkills,
                // а Link является частью Sequence, которая
                // может быть обработана доменом позже
            }

            return result;
        }

        private LeafEngagedSkills extractLeafSkills(DecisionTreeTrace trace) {

            // Берем последний элемент ветви трассы.
            // Не используем resultingElement, так как resultingElement может быть
            // не последним элементом (см комментарий к resultingElement).
            // Cast безопасен, так как аналогичная проверка выполняется в конструкторе DecisionTreeTrace
            @SuppressWarnings("unchecked")
            DecisionTreeTraceElement<BranchResult, ?> leafElem =
                    (DecisionTreeTraceElement<BranchResult, ?>) trace.getLast();

            BranchResult leafResult = leafElem.getNodeResult();
            Set<String> correctlyApplied = new HashSet<>();
            Set<String> violated = new HashSet<>();

            // Получаем навыки из узла
            Object skillsMeta = Optional.of(leafElem)
                    .map(DecisionTreeTraceElement::getNode)
                    .map(DecisionTreeElement::getMetadata)
                    .filter(m -> m.containsAny("skill"))
                    .map(m -> m.get("skill"))
                    .orElse(null);

            if (skillsMeta instanceof String str && !str.isBlank()) {
                List<String> skills = Arrays.asList(str.split(";"));
                switch (leafResult) {
                    case CORRECT -> correctlyApplied.addAll(skills);
                    case ERROR -> violated.addAll(skills);
                    case NULL -> { /* ничего не делаем */ }
                }
            }

            return new LeafEngagedSkills(leafResult, correctlyApplied, violated);
        }

        private LeafEngagedSkills processAggregation(
                AggregationDecisionTreeTraceElement<?> aggElem
        ) {

            List<LeafEngagedSkills> branchesResults = aggElem.nestedTraces()
                    .stream()
                    .map(this::extract)
                    .toList();

            return mergeBranches(aggElem.getNodeResult(), branchesResults);
        }

        private LeafEngagedSkills processCycle(
                WhileCycleDecisionTreeTraceElement cycleElem
        ) {

            List<LeafEngagedSkills> iterResults = cycleElem.getBranchTraceList()
                    .stream()
                    .map(this::extract)
                    .toList();

            return mergeBranches(cycleElem.getNodeResult(), iterResults);
        }

        private LeafEngagedSkills mergeBranches(
                BranchResult nodeResult,
                List<LeafEngagedSkills> branches
        ) {

            Set<String> correctlyApplied = new HashSet<>();
            Set<String> violated = new HashSet<>();

            switch (nodeResult) {
                case CORRECT -> branches.stream()
                        .filter(b -> b.branchResult == BranchResult.CORRECT)
                        .forEach(b -> correctlyApplied.addAll(b.correctlyApplied));

                case ERROR -> branches.stream()
                        .filter(b -> b.branchResult == BranchResult.ERROR)
                        .forEach(b -> violated.addAll(b.violated));

                case NULL -> { /* ничего не делаем */ }
            }

            return new LeafEngagedSkills(nodeResult, correctlyApplied, violated);
        }
    }
}
