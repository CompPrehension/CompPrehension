package org.vstu.compprehension.businesslogic.domains;

import its.model.nodes.DecisionTreeElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class DecisionTreeElements {

    private DecisionTreeElements() {
    }

    static List<DecisionTreeElement> of(DecisionTreeReasoningDomain domain) {
        var elements = new ArrayList<DecisionTreeElement>();
        for (var model : domain.getDomainSolvingModels()) {
            for (var tree : model.getDecisionTrees().values()) {
                collect(tree, elements, new HashSet<>());
            }
        }
        return elements;
    }

    private static void collect(DecisionTreeElement element, List<DecisionTreeElement> into, Set<DecisionTreeElement> seen) {
        if (!seen.add(element)) {
            return;
        }
        into.add(element);
        for (var linked : element.getLinkedElements()) {
            collect(linked, into, seen);
        }
    }
}
