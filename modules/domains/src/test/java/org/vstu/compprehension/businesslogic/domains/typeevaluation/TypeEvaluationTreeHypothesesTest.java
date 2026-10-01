package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.nodes.AggregationMethod;
import its.model.nodes.BranchAggregationNode;
import its.model.nodes.BranchResult;
import its.model.nodes.BranchResultNode;
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
                .flatMap(aggregation -> aggregation.getThoughtBranches().stream())
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
        var ambiguous = ambiguousAggregations();

        // Act.
        var withoutExplanation = ambiguous.stream()
                .filter(aggregation -> aggregation.getMetadata().get("RU", "explanation") == null
                        || aggregation.getMetadata().get("EN", "explanation") == null)
                .map(aggregation -> aggregation.getThoughtBranches().stream()
                        .flatMap(branch -> conclusions(branch.getStart()).stream())
                        .map(conclusion -> conclusion.getMetadata().getString("hypothesis"))
                        .toList()
                        .toString())
                .toList();

        // Assert.
        assertFalse(ambiguous.isEmpty());
        assertEquals(List.of(), withoutExplanation);
    }

    /** У каждого заблуждения, которое может разделить ошибку с другим, есть причина для уточняющего вопроса студенту. */
    @Test
    void ambiguousErrorHypothesesHaveReasons() {
        // Arrange.
        var ambiguousErrors = ambiguousAggregations().stream()
                .flatMap(aggregation -> aggregation.getThoughtBranches().stream())
                .flatMap(branch -> conclusions(branch.getStart()).stream())
                .filter(conclusion -> conclusion.getValue() == BranchResult.ERROR)
                .toList();

        // Act.
        var withoutReason = ambiguousErrors.stream()
                .filter(conclusion -> conclusion.getMetadata().get("RU", "reason") == null
                        || conclusion.getMetadata().get("EN", "reason") == null)
                .map(conclusion -> conclusion.getMetadata().getString("hypothesis"))
                .toList();

        // Assert.
        assertFalse(ambiguousErrors.isEmpty());
        assertEquals(List.of(), withoutReason);
    }

    private static @NotNull List<BranchAggregationNode> ambiguousAggregations() {
        return hypothesisAggregations().stream()
                .filter(aggregation -> aggregation.getThoughtBranches().stream()
                        .filter(branch -> conclusions(branch.getStart()).stream()
                                .anyMatch(conclusion -> conclusion.getValue() == BranchResult.ERROR))
                        .count() > 1)
                .toList();
    }

    private static @NotNull List<BranchAggregationNode> hypothesisAggregations() {
        var found = new ArrayList<BranchAggregationNode>();
        collectHypothesisAggregations(TypeEvaluationTreeFixture.tree().getMainBranch().getStart(), found);
        return found;
    }

    private static void collectHypothesisAggregations(@NotNull DecisionTreeNode node,
                                                      @NotNull List<BranchAggregationNode> found) {
        if (node instanceof BranchAggregationNode aggregation) {
            if (aggregation.getAggregationMethod() == AggregationMethod.HYP) {
                found.add(aggregation);
            }
            for (ThoughtBranch branch : aggregation.getThoughtBranches()) {
                collectHypothesisAggregations(branch.getStart(), found);
            }
            for (Outcome<BranchResult> outcome : aggregation.getOutcomes()) {
                collectHypothesisAggregations(outcome.getNode(), found);
            }
        } else if (node instanceof LinkNode<?> link) {
            for (Outcome<?> outcome : link.getOutcomes()) {
                collectHypothesisAggregations(outcome.getNode(), found);
            }
        }
    }

    /** Выводы correct/error ветви рассуждения. */
    private static @NotNull List<BranchResultNode> conclusions(@NotNull DecisionTreeNode node) {
        if (node instanceof BranchResultNode result) {
            return result.getValue() == BranchResult.NULL ? List.of() : List.of(result);
        }
        var found = new ArrayList<BranchResultNode>();
        if (node instanceof LinkNode<?> link) {
            for (Outcome<?> outcome : link.getOutcomes()) {
                found.addAll(conclusions(outcome.getNode()));
            }
        }
        return found;
    }
}
