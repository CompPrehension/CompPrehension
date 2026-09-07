package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.iterators.DFSNodeIterator;
import org.vstu.meaningtree.iterators.utils.NodeInfo;
import org.vstu.meaningtree.nodes.expressions.comparison.CompoundComparison;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TreeMutationGenerator {
    private static final List<Mutation> requiredMutations = List.of(
            PredefinedMutations.unaryMutation, PredefinedMutations.parenRemove,
            PredefinedMutations.ternaryComplexify
    );

    private final MeaningTree mt;
    private Map<NodeInfo, List<Mutation>> mutationNodes;
    private final ArrayList<MeaningTree> mutatedTrees = new ArrayList<>();

    TreeMutationGenerator(MeaningTree source) {
        this.mt = source;
        mutatedTrees.add(mt);
    }

    private void analyze() {
        mutationNodes = new HashMap<>();
        DFSNodeIterator dfs = new DFSNodeIterator(mt.getRootNode(), true);
        dfs.addEnterCondition((node) -> !(node instanceof CompoundComparison));
        for (NodeInfo node : dfs) {
            if (node == null) continue;
            List<Mutation> mutList = requiredMutations.stream().filter(mut -> mut.isEligible(node)).toList();
            if (!mutList.isEmpty()) mutationNodes.put(node, mutList);
        }
    }

    private void process() {
        for (var entry : mutationNodes.entrySet()) {
            for (var mut : entry.getValue()) {
                MeaningTree newMt = mut.apply(mt, entry.getKey());
                if (newMt != null) mutatedTrees.add(newMt);
            }
        }
    }

    public List<MeaningTree> generate() {
        analyze();
        process();
        return mutatedTrees;
    }

}
