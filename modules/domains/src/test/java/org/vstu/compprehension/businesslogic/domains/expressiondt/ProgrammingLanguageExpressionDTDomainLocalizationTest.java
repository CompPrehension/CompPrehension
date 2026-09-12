package org.vstu.compprehension.businesslogic.domains.expressiondt;

import org.junit.jupiter.api.Test;
import org.vstu.compprehension.businesslogic.domains.DecisionTreeDomainLocalizationContract;
import org.vstu.compprehension.businesslogic.domains.DecisionTreeReasoningDomain;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgrammingLanguageExpressionDTDomainLocalizationTest extends DecisionTreeDomainLocalizationContract {

    @Override
    protected DecisionTreeReasoningDomain domain() {
        return ExpressionDtDomainFixture.domain();
    }

    @Override
    protected List<String> messageBundles() {
        return ExpressionDtDomainFixture.BUNDLES;
    }

    /** Загружаются все три дерева домена. */
    @Test
    void allDecisionTreesAreLoaded() {
        // Act.
        var trees = domain().getDomainSolvingModels().getFirst().getDecisionTrees();

        // Assert.
        assertTrue(trees.size() >= 3, trees.keySet().toString());
        assertTrue(decisionTreeElements().size() > 100);
    }
}
