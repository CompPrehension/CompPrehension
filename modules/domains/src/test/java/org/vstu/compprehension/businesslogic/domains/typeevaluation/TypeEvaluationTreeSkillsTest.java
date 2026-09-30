package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.nodes.BranchAggregationNode;
import its.model.nodes.BranchResult;
import its.model.nodes.BranchResultNode;
import its.model.nodes.DecisionTreeNode;
import its.model.nodes.LinkNode;
import its.model.nodes.Outcome;
import its.model.nodes.ThoughtBranch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeEvaluationTreeSkillsTest {

    /** Навык каждого вывода correct/error — навык последней развилки перед ним, у которой к таким выводам ведут хотя бы два выхода. */
    @Test
    void conclusionSkillIsSkillOfItsLastFork() {
        // Act.
        var walk = SkillWalk.of(TypeEvaluationTreeFixture.tree().getMainBranch());

        // Assert.
        assertTrue(walk.checkedConclusions > 0);
        assertEquals(List.of(), walk.mismatches);
    }

    /** Навыком размечены ровно те развилки, которые оказываются последними перед каким-либо выводом. */
    @Test
    void annotatedForksAreExactlyLastForks() {
        // Act.
        var walk = SkillWalk.of(TypeEvaluationTreeFixture.tree().getMainBranch());

        // Assert.
        assertEquals(List.of(), walk.annotatedForksNeverLast());
    }

    /** Обход дерева проверок с запоминанием последней развилки на пути. */
    private static final class SkillWalk {
        private final List<String> mismatches = new ArrayList<>();
        private final Set<DecisionTreeNode> lastForks = new HashSet<>();
        private final Set<DecisionTreeNode> annotatedForks = new HashSet<>();
        private final Map<DecisionTreeNode, Boolean> reachesVerdict = new HashMap<>();
        private int checkedConclusions;

        static @NotNull SkillWalk of(@NotNull ThoughtBranch mainBranch) {
            var walk = new SkillWalk();
            walk.visit(mainBranch.getStart(), null, false);
            return walk;
        }

        private void visit(@NotNull DecisionTreeNode node, @Nullable DecisionTreeNode lastFork, boolean relaysAggregation) {
            if (node instanceof BranchResultNode result) {
                if (result.getValue() != BranchResult.NULL && !relaysAggregation) {
                    checkConclusion(result, lastFork);
                }
                return;
            }
            if (node instanceof BranchAggregationNode aggregation) {
                for (ThoughtBranch branch : aggregation.getThoughtBranches()) {
                    visit(branch.getStart(), lastFork, false);
                }
                // Выводы по исходам correct/error лишь передают итог hyp наверх и своего навыка не несут.
                for (Outcome<BranchResult> outcome : aggregation.getOutcomes()) {
                    visit(outcome.getNode(), lastFork, outcome.getKey() != BranchResult.NULL);
                }
                return;
            }
            if (node instanceof LinkNode<?> link) {
                if (skillOf(link) != null) {
                    annotatedForks.add(link);
                }
                var fork = isFork(link) ? link : lastFork;
                for (Outcome<?> outcome : link.getOutcomes()) {
                    visit(outcome.getNode(), fork, false);
                }
                return;
            }
            throw new IllegalStateException("Неподдерживаемый узел дерева: " + node);
        }

        private void checkConclusion(@NotNull BranchResultNode conclusion, @Nullable DecisionTreeNode lastFork) {
            checkedConclusions++;
            var expected = lastFork == null ? null : skillOf(lastFork);
            if (lastFork != null) {
                lastForks.add(lastFork);
            }
            var actual = skillOf(conclusion);
            if (expected == null || !expected.equals(actual)) {
                mismatches.add("%s [hypothesis=%s]: навык %s, у последней развилки — %s".formatted(
                        conclusion.getValue(), conclusion.getMetadata().getString("hypothesis"), actual, expected));
            }
        }

        private boolean isFork(@NotNull LinkNode<?> link) {
            int outcomesWithVerdict = 0;
            for (Outcome<?> outcome : link.getOutcomes()) {
                if (reachesVerdict(outcome.getNode())) {
                    outcomesWithVerdict++;
                }
            }
            return outcomesWithVerdict >= 2;
        }

        private boolean reachesVerdict(@NotNull DecisionTreeNode node) {
            var known = reachesVerdict.get(node);
            if (known != null) {
                return known;
            }
            boolean reaches = false;
            if (node instanceof BranchResultNode result) {
                reaches = result.getValue() != BranchResult.NULL;
            } else if (node instanceof BranchAggregationNode aggregation) {
                reaches = aggregation.getThoughtBranches().stream().anyMatch(branch -> reachesVerdict(branch.getStart()))
                        || aggregation.getOutcomes().stream().anyMatch(outcome -> reachesVerdict(outcome.getNode()));
            } else if (node instanceof LinkNode<?> link) {
                for (Outcome<?> outcome : link.getOutcomes()) {
                    reaches |= reachesVerdict(outcome.getNode());
                }
            }
            reachesVerdict.put(node, reaches);
            return reaches;
        }

        private static @Nullable String skillOf(@NotNull DecisionTreeNode node) {
            return node.getMetadata().getString("skill");
        }

        private @NotNull List<String> annotatedForksNeverLast() {
            return annotatedForks.stream()
                    .filter(fork -> !lastForks.contains(fork))
                    .map(SkillWalk::skillOf)
                    .sorted()
                    .toList();
        }
    }
}
