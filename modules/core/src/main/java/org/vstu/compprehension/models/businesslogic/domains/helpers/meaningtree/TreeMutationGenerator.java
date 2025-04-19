package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.nodes.Node;

import java.util.*;

class TreeMutationGenerator {
    private static final List<Mutation> requiredMutations = List.of(
            PredefinedMutations.unaryMutation, PredefinedMutations.parenRemove,
            PredefinedMutations.ternaryComplexify
    );

    private final MeaningTree mt;
    private Map<Node.Info, List<Mutation>> mutationNodes;
    private final ArrayList<MeaningTree> mutatedTrees = new ArrayList<>();

    TreeMutationGenerator(MeaningTree source) {
        this.mt = source;
    }

    private void analyze() {
        mutationNodes = new HashMap<>();
        for (Node.Info node : mt) {
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
        return mutatedTrees.isEmpty() ? List.of(mt) : mutatedTrees;
    }

}
