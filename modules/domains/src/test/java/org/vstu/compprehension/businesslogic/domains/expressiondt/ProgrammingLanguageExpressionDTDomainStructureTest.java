package org.vstu.compprehension.businesslogic.domains;

import org.junit.jupiter.api.Test;
import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.Law;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.enums.InteractionType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.ExpressionDtDomainFixture.CPP_TAG;
import static org.vstu.compprehension.businesslogic.domains.ExpressionDtDomainFixture.domain;

class ProgrammingLanguageExpressionDTDomainStructureTest {

    private static final String PRECEDENCE_SKILL = "order_determined_by_precedence";
    private static final String ARITHMETICS_CONCEPT = "arithmetics";
    private static final String BINARY_PLUS_CONCEPT = "operator_binary_+";
    private static final String HIGHER_PRECEDENCE_LAW = "error_base_higher_precedence_left";

    /** Идентификаторы домена. */
    @Test
    void domainIdentity() {
        // Act & Assert.
        assertEquals("ProgrammingLanguageExpressionDTDomain", domain().getDomainId());
        assertEquals("expression_dt", domain().getShortnameForQuestionSearch());
        assertEquals("DTReasoner", domain().getBackendId());
        assertFalse(domain().requiresSolving());
        assertNotNull(domain().getDefaultQuestionType(false));
    }

    /** Умения: непустой набор с уникальными битами и деревом для преподавателя. */
    @Test
    void skillsAreDefinedWithUniqueBits() {
        // Act.
        var skills = domain().getAllSkills();
        var teacherSkills = domain().getSkillSimplifiedHierarchy(Skill.FLAG_VISIBLE_TO_TEACHER);

        // Assert.
        assertFalse(skills.isEmpty());
        assertUniqueBits(skills.stream().map(Skill::getBitmask).toList());
        assertFalse(teacherSkills.isEmpty());
        assertTrue(teacherSkills.keySet().stream().allMatch(s -> s.hasFlag(Skill.FLAG_VISIBLE_TO_TEACHER)));
        var precedence = domain().getSkill(PRECEDENCE_SKILL);
        assertNotNull(precedence);
        assertTrue(precedence.getBitmask() != 0);
        assertEquals(List.of(precedence), domain().skillsFromBitmask(precedence.getBitmask()));
        assertNull(domain().getSkill("no_such_skill"));
    }

    /** Концепты: иерархия, дочерние и биты. */
    @Test
    void conceptsFormHierarchyWithBits() {
        // Act.
        var concepts = domain().getConcepts();
        var arithmetics = domain().getConcept(ARITHMETICS_CONCEPT);
        var binaryPlus = domain().getConcept(BINARY_PLUS_CONCEPT);
        var withChildren = domain().getConceptWithChildren(ARITHMETICS_CONCEPT);

        // Assert.
        assertFalse(concepts.isEmpty());
        assertNotNull(arithmetics);
        assertNotNull(binaryPlus);
        assertTrue(arithmetics.hasFlag(Concept.FLAG_VISIBLE_TO_TEACHER));
        assertTrue(binaryPlus.hasBaseConcept(arithmetics));
        assertTrue(withChildren.contains(arithmetics));
        assertTrue(withChildren.contains(binaryPlus));
        assertTrue(binaryPlus.getBitmask() != 0);
        assertTrue((arithmetics.getSubTreeBitmask() & binaryPlus.getBitmask()) != 0);
        assertEquals(List.of(binaryPlus), domain().conceptsFromBitmask(binaryPlus.getBitmask()));
        assertFalse(domain().getConceptsSimplifiedHierarchy(Concept.FLAG_VISIBLE_TO_TEACHER).isEmpty());
        assertNull(domain().getConcept("no_such_concept"));
    }

    /** Законы: негативные с битами, дерево для преподавателя. */
    @Test
    void lawsAreDefinedWithBits() {
        // Act.
        var negativeLaws = domain().getNegativeLaws();
        var positiveLaws = domain().getPositiveLaws();
        var law = domain().getNegativeLaw(HIGHER_PRECEDENCE_LAW);

        // Assert.
        assertFalse(negativeLaws.isEmpty());
        assertFalse(positiveLaws.isEmpty());
        assertNotNull(law);
        assertEquals(law, domain().getLaw(HIGHER_PRECEDENCE_LAW));
        assertTrue(law.getBitmask() != 0);
        assertEquals(List.of(law), domain().negativeLawFromBitmask(law.getBitmask()));
        assertFalse(domain().getLawsSimplifiedHierarchy(Law.FLAG_VISIBLE_TO_TEACHER).isEmpty());
        assertNull(domain().getLaw("no_such_law"));
    }

    /** Теги: языки программирования с уникальными битами. */
    @Test
    void tagsCoverProgrammingLanguages() {
        // Act.
        var tags = domain().getTags();

        // Assert.
        assertTrue(tags.keySet().containsAll(Set.of(CPP_TAG, "Python", "Java")));
        assertUniqueBits(tags.values().stream().map(t -> t.getBitmask()).toList());
        assertEquals(tags.get(CPP_TAG), domain().getTag(CPP_TAG));
        assertEquals(List.of(tags.get(CPP_TAG)), domain().resolveTags(List.of(CPP_TAG, "no-such-tag")));
        assertNull(domain().getTag("no-such-tag"));
    }

    /** Доп. вопросы предлагаются по умениям, но не после подсказки. */
    @Test
    void supplementaryQuestionsAreOfferedForSkillsOnly() {
        // Act & Assert.
        assertTrue(domain().needSupplementaryQuestion(PRECEDENCE_SKILL, InteractionType.SEND_RESPONSE));
        assertFalse(domain().needSupplementaryQuestion(PRECEDENCE_SKILL, InteractionType.REQUEST_CORRECT_ANSWER));
        assertFalse(domain().needSupplementaryQuestion("stillUnevaluatedLeft", InteractionType.SEND_RESPONSE));
    }

    private static void assertUniqueBits(List<Long> bits) {
        var nonZero = bits.stream().filter(b -> b != 0).toList();
        assertFalse(nonZero.isEmpty());
        assertEquals(nonZero.size(), new HashSet<>(nonZero).size(), "повторяющиеся биты: " + duplicates(nonZero));
    }

    private static Set<Long> duplicates(List<Long> bits) {
        var seen = new HashSet<Long>();
        return bits.stream().filter(b -> !seen.add(b)).collect(Collectors.toSet());
    }
}
