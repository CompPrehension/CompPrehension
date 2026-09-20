package org.vstu.compprehension.businesslogic.domains;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public abstract class DecisionTreeDomainStructureContract extends DomainStructureContract {

    @Override
    protected abstract DecisionTreeReasoningDomain domain();

    /** Все умения из деревьев решений зарегистрированы в домене. */
    @Test
    protected void decisionTreeSkillsAreKnownToDomain() {
        // Arrange.
        var treeSkills = new TreeSet<String>();
        for (var element : DecisionTreeElements.of(domain())) {
            var metadata = element.getMetadata();
            if (metadata.containsAny("skill")) {
                Arrays.stream(metadata.getString("skill").split(";"))
                        .map(String::trim)
                        .filter(skill -> !skill.isEmpty())
                        .forEach(treeSkills::add);
            }
        }

        // Act.
        var unknown = treeSkills.stream().filter(skill -> domain().getSkill(skill) == null).toList();

        // Assert.
        assertFalse(treeSkills.isEmpty());
        assertEquals(List.of(), unknown);
    }
}
