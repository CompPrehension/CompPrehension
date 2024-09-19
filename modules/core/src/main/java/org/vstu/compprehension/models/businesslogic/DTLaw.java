package org.vstu.compprehension.models.businesslogic;

import its.model.nodes.DecisionTree;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * TODO Class Description
 *
 * @author Marat Gumerov
 * @since 28.01.2024
 */
@Getter
public class DTLaw extends NegativeLaw {
    private final DecisionTree decisionTree;

    public DTLaw(DecisionTree decisionTree) {
        super(
            decisionTree.getDescription(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            ""
        );
        this.decisionTree = decisionTree;
    }
}
