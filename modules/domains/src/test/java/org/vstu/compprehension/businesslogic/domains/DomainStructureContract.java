package org.vstu.compprehension.businesslogic.domains;

import org.junit.jupiter.api.Test;
import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.Law;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.businesslogic.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class DomainStructureContract {

    protected abstract DomainBase domain();

    /** Умения: уникальные биты, поиск по имени и обратно по маске. */
    @Test
    protected void skillsHaveUniqueBitsAndRoundTripThroughBitmask() {
        // Act.
        var skills = domain().getAllSkills();

        // Assert.
        assertFalse(skills.isEmpty());
        assertUniqueBits(skills.stream().map(Skill::getBitmask).toList());
        for (var skill : skills) {
            assertEquals(skill, domain().getSkill(skill.getName()));
            if (skill.getBitmask() != 0) {
                assertEquals(List.of(skill), domain().skillsFromBitmask(skill.getBitmask()), skill.getName());
            }
        }
        assertNull(domain().getSkill("no_such_skill"));
    }

    /** Концепты: поиск по имени и обратно по маске (синонимы делят бит). */
    @Test
    protected void conceptsRoundTripThroughBitmask() {
        // Act.
        var concepts = domain().getConcepts();

        // Assert.
        assertFalse(concepts.isEmpty());
        for (var concept : concepts) {
            assertEquals(concept, domain().getConcept(concept.getName()));
            if (concept.getBitmask() != 0) {
                assertTrue(domain().conceptsFromBitmask(concept.getBitmask()).contains(concept), concept.getName());
            }
        }
        assertNull(domain().getConcept("no_such_concept"));
    }

    /** Дочерние концепты достижимы из базовых и входят в маску поддерева. */
    @Test
    protected void childConceptsAreReachableFromBaseConcepts() {
        // Arrange.
        var withBase = domain().getConcepts().stream()
                .filter(c -> c.getBaseConcepts() != null && !c.getBaseConcepts().isEmpty())
                .toList();

        // Act & Assert.
        assertFalse(withBase.isEmpty());
        for (var concept : withBase) {
            for (var base : concept.getBaseConcepts()) {
                assertTrue(concept.hasBaseConcept(base));
                assertTrue(domain().getConceptWithChildren(base.getName()).contains(concept), concept.getName() + " под " + base.getName());
                assertTrue(domain().getChildrenOfConcept(base.getName()).contains(concept), concept.getName() + " под " + base.getName());
                if (concept.getBitmask() != 0) {
                    assertNotEquals(0, base.getSubTreeBitmask() & concept.getBitmask(), concept.getName() + " в маске " + base.getName());
                }
            }
        }
    }

    /** Негативные законы: уникальные биты, поиск по имени и обратно по маске. */
    @Test
    protected void negativeLawsRoundTripThroughBitmask() {
        // Act.
        var laws = domain().getNegativeLaws();

        // Assert.
        assertUniqueBits(laws.stream().map(Law::getBitmask).toList());
        for (var law : laws) {
            assertEquals(law, domain().getLaw(law.getName()));
            assertEquals(law, domain().getNegativeLaw(law.getName()));
            if (law.getBitmask() != 0) {
                assertEquals(List.of(law), domain().negativeLawFromBitmask(law.getBitmask()), law.getName());
            }
        }
        assertNull(domain().getLaw("no_such_law"));
        assertNull(domain().getNegativeLaw("no_such_law"));
    }

    /** Теги: уникальные биты и поиск по имени. */
    @Test
    protected void tagsHaveUniqueBitsAndResolveByName() {
        // Arrange.
        var tags = domain().getTags();
        var names = new ArrayList<>(tags.keySet());
        names.add("no-such-tag");

        // Act.
        var resolved = domain().resolveTags(names);

        // Assert.
        assertFalse(tags.isEmpty());
        assertUniqueBits(tags.values().stream().map(Tag::getBitmask).toList());
        tags.forEach((name, tag) -> {
            assertEquals(name, tag.getName());
            assertEquals(tag, domain().getTag(name));
        });
        assertEquals(tags.keySet().stream().map(tags::get).toList(), resolved);
        assertNull(domain().getTag("no-such-tag"));
    }

    /** Иерархии для преподавателя состоят только из видимых ему элементов. */
    @Test
    protected void teacherHierarchiesContainOnlyTeacherVisibleRoots() {
        // Act.
        var skills = domain().getSkillSimplifiedHierarchy(Skill.FLAG_VISIBLE_TO_TEACHER);
        var concepts = domain().getConceptsSimplifiedHierarchy(Concept.FLAG_VISIBLE_TO_TEACHER);
        var laws = domain().getLawsSimplifiedHierarchy(Law.FLAG_VISIBLE_TO_TEACHER);

        // Assert.
        assertFalse(skills.isEmpty());
        assertFalse(concepts.isEmpty());
        assertTrue(skills.keySet().stream().allMatch(s -> s.hasFlag(Skill.FLAG_VISIBLE_TO_TEACHER)));
        assertTrue(concepts.keySet().stream().allMatch(c -> c.hasFlag(Concept.FLAG_VISIBLE_TO_TEACHER)));
        assertTrue(laws.keySet().stream().allMatch(l -> l.hasFlag(Law.FLAG_VISIBLE_TO_TEACHER)));
        assertTrue(concepts.values().stream().flatMap(List::stream).allMatch(c -> c.hasFlag(Concept.FLAG_VISIBLE_TO_TEACHER)));
    }

    protected static void assertUniqueBits(List<Long> bits) {
        var nonZero = bits.stream().filter(b -> b != 0).toList();
        assertEquals(nonZero.size(), new HashSet<>(nonZero).size(), "повторяющиеся биты: " + duplicates(nonZero));
    }

    private static Set<Long> duplicates(List<Long> bits) {
        var seen = new HashSet<Long>();
        return bits.stream().filter(b -> !seen.add(b)).collect(Collectors.toSet());
    }
}
