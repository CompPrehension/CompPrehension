package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.nodes.DecisionTreeElement;
import org.junit.jupiter.api.Test;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.businesslogic.domains.DecisionTreeDomainStructureContract;
import org.vstu.compprehension.businesslogic.domains.DecisionTreeReasoningDomain;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeEvaluationDTDomainStructureTest extends DecisionTreeDomainStructureContract {

    @Override
    protected DecisionTreeReasoningDomain domain() {
        return TypeEvaluationDomainFixture.domain();
    }

    /** Реестр умений домена совпадает с умениями, которыми размечено дерево, в обе стороны. */
    @Test
    void registeredSkillsAreExactlyTreeSkills() {
        // Arrange.
        var treeSkills = new TreeSet<String>();
        var queue = new ArrayDeque<DecisionTreeElement>();
        var seen = new HashSet<DecisionTreeElement>();
        queue.add(TypeEvaluationTreeFixture.tree());
        while (!queue.isEmpty()) {
            var element = queue.poll();
            if (seen.add(element)) {
                var skill = element.getMetadata().getString("skill");
                if (skill != null) {
                    treeSkills.add(skill);
                }
                queue.addAll(element.getLinkedElements());
            }
        }

        // Act.
        var registered = domain().getAllSkills().stream()
                .map(Skill::getName)
                .collect(Collectors.toCollection(TreeSet::new));

        // Assert.
        assertEquals(treeSkills, registered);
    }
}
