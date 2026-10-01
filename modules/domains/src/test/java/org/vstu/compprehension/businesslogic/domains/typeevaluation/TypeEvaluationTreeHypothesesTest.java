package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.nodes.AggregationMethod;
import its.model.nodes.AggregationNode;
import its.model.nodes.BranchAggregationNode;
import its.model.nodes.BranchResult;
import its.model.nodes.BranchResultNode;
import its.model.nodes.CycleAggregationNode;
import its.model.nodes.DecisionTreeNode;
import its.model.nodes.LinkNode;
import its.model.nodes.Outcome;
import its.model.nodes.ThoughtBranch;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TypeEvaluationTreeHypothesesTest {

    /** Каждый вывод гипотезы назван: иначе ответ студента нельзя отнести к способу рассуждения. */
    @Test
    void everyHypothesisConclusionIsNamed() {
        // Arrange.
        var hypotheses = hypothesisAggregations();

        // Act.
        var unnamed = hypotheses.stream()
                .flatMap(aggregation -> branchesOf(aggregation).stream())
                .flatMap(branch -> conclusions(branch.getStart()).stream())
                .filter(conclusion -> conclusion.getMetadata().getString("hypothesis") == null)
                .map(conclusion -> conclusion.getValue() + " " + conclusion.getMetadata().getString("skill"))
                .toList();

        // Assert.
        assertFalse(hypotheses.isEmpty());
        assertEquals(List.of(), unnamed);
    }

    /**
     * Если одну ошибку могут объяснить несколько заблуждений, у hyp есть общее объяснение на обоих языках:
     * иначе студент увидит несколько взаимоисключающих объяснений.
     */
    @Test
    void ambiguousHypothesesHaveGeneralExplanation() {
        // Arrange.
        var ambiguous = hypothesisAggregations().stream()
                .filter(aggregation -> branchesOf(aggregation).stream()
                        .filter(branch -> conclusions(branch.getStart()).stream()
                                .anyMatch(conclusion -> conclusion.getValue() == BranchResult.ERROR))
                        .count() > 1)
                .toList();

        // Act.
        var withoutExplanation = ambiguous.stream()
                .filter(aggregation -> aggregation.getMetadata().get("RU", "explanation") == null
                        || aggregation.getMetadata().get("EN", "explanation") == null)
                .map(aggregation -> branchesOf(aggregation).stream()
                        .flatMap(branch -> conclusions(branch.getStart()).stream())
                        .map(conclusion -> conclusion.getMetadata().getString("hypothesis"))
                        .toList()
                        .toString())
                .toList();

        // Assert.
        assertFalse(ambiguous.isEmpty());
        assertEquals(List.of(), withoutExplanation);
    }

    /**
     * У каждого заблуждения и каждой ошибки чтения есть причина для уточняющего вопроса: ошибка чтения может
     * совпасть с любым заблуждением, поэтому неоднозначным может стать любой ответ.
     */
    @Test
    void everyErrorHypothesisHasReason() {
        // Arrange.
        var errors = hypothesisAggregations().stream()
                .flatMap(aggregation -> branchesOf(aggregation).stream())
                .flatMap(branch -> conclusions(branch.getStart()).stream())
                .filter(conclusion -> conclusion.getValue() == BranchResult.ERROR)
                .distinct()
                .toList();

        // Act.
        var withoutReason = errors.stream()
                .filter(conclusion -> conclusion.getMetadata().get("RU", "reason") == null
                        || conclusion.getMetadata().get("EN", "reason") == null)
                .map(conclusion -> conclusion.getMetadata().getString("hypothesis"))
                .toList();

        // Assert.
        assertFalse(errors.isEmpty());
        assertEquals(List.of(), withoutReason);
    }

    private static @NotNull List<AggregationNode> hypothesisAggregations() {
        var found = new ArrayList<AggregationNode>();
        for (var tree : TypeEvaluationTreeFixture.trees()) {
            collectHypothesisAggregations(tree.getMainBranch().getStart(), found);
        }
        return found;
    }

    private static void collectHypothesisAggregations(@NotNull DecisionTreeNode node, @NotNull List<AggregationNode> found) {
        if (node instanceof AggregationNode aggregation && aggregation.getAggregationMethod() == AggregationMethod.HYP) {
            found.add(aggregation);
        }
        if (node instanceof AggregationNode aggregation) {
            for (ThoughtBranch branch : branchesOf(aggregation)) {
                collectHypothesisAggregations(branch.getStart(), found);
            }
        }
        if (node instanceof LinkNode<?> link) {
            for (Outcome<?> outcome : link.getOutcomes()) {
                collectHypothesisAggregations(outcome.getNode(), found);
            }
        }
    }

    private static @NotNull List<ThoughtBranch> branchesOf(@NotNull AggregationNode aggregation) {
        if (aggregation instanceof BranchAggregationNode branches) {
            return branches.getThoughtBranches();
        }
        if (aggregation instanceof CycleAggregationNode cycle) {
            return List.of(cycle.getThoughtBranch());
        }
        throw new IllegalStateException("Неподдерживаемая агрегация: " + aggregation);
    }

    /** Выводы correct/error ветви рассуждения; выходы агрегаций лишь передают её итог и не в счёт. */
    private static @NotNull List<BranchResultNode> conclusions(@NotNull DecisionTreeNode node) {
        if (node instanceof BranchResultNode result) {
            return result.getValue() == BranchResult.NULL ? List.of() : List.of(result);
        }
        var found = new ArrayList<BranchResultNode>();
        if (node instanceof AggregationNode aggregation) {
            for (ThoughtBranch branch : branchesOf(aggregation)) {
                found.addAll(conclusions(branch.getStart()));
            }
        } else if (node instanceof LinkNode<?> link) {
            for (Outcome<?> outcome : link.getOutcomes()) {
                found.addAll(conclusions(outcome.getNode()));
            }
        }
        return found;
    }
}
