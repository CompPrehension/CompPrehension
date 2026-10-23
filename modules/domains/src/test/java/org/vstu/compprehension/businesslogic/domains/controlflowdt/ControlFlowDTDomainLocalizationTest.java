package org.vstu.compprehension.businesslogic.domains.controlflowdt;

import its.model.nodes.BranchResult;
import its.model.nodes.BranchResultNode;
import org.junit.jupiter.api.Test;
import org.vstu.compprehension.businesslogic.domains.DecisionTreeDomainLocalizationContract;
import org.vstu.compprehension.businesslogic.domains.DecisionTreeReasoningDomain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlFlowDTDomainLocalizationTest extends DecisionTreeDomainLocalizationContract {

    @Override
    protected DecisionTreeReasoningDomain domain() {
        return ControlFlowDtDomainFixture.domain();
    }

    @Override
    protected List<String> messageBundles() {
        return ControlFlowDtDomainFixture.BUNDLES;
    }

    @Override
    protected boolean isLanguageSpecificMessageKey(String key) {
        return key.endsWith(".he") || key.endsWith(".she") || key.endsWith("_program");
    }

    /** Загружается единственное дерево домена. */
    @Test
    void decisionTreeIsLoaded() {
        // Act.
        var trees = domain().getDomainSolvingModels().getFirst().getDecisionTrees();

        // Assert.
        assertEquals(1, trees.size(), trees.keySet().toString());
        assertTrue(decisionTreeElements().size() > 50);
    }

    /** У каждой ошибки с умением есть русское объяснение. */
    @Test
    void everySkillErrorHasRussianExplanation() {
        // Arrange.
        var errors = decisionTreeElements().stream()
                .filter(element -> element instanceof BranchResultNode node && node.getValue() == BranchResult.ERROR)
                .filter(element -> element.getMetadata().containsAny("skill"))
                .toList();

        // Act.
        var withoutExplanation = new ArrayList<String>();
        for (var error : errors) {
            if (!localizedProperties(error).getOrDefault("RU", Set.of()).contains("explanation")) {
                withoutExplanation.add(error.getMetadata().get("skill") + " at " + describe(error));
            }
        }

        // Assert.
        assertTrue(errors.size() > 10);
        assertEquals(List.of(), withoutExplanation);
    }
}
