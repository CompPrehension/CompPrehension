package org.vstu.compprehension.businesslogic.domains.expressiondt;

import org.junit.jupiter.api.Test;
import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.businesslogic.domains.DomainBase;
import org.vstu.compprehension.businesslogic.domains.DomainStructureContract;
import org.vstu.compprehension.enums.InteractionType;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.CPP_TAG;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.domain;

class ProgrammingLanguageExpressionDTDomainStructureTest extends DomainStructureContract {

    private static final String PRECEDENCE_SKILL = "order_determined_by_precedence";
    private static final String ARITHMETICS_CONCEPT = "arithmetics";
    private static final String BINARY_PLUS_CONCEPT = "operator_binary_+";
    private static final String HIGHER_PRECEDENCE_LAW = "error_base_higher_precedence_left";

    @Override
    protected DomainBase domain() {
        return ExpressionDtDomainFixture.domain();
    }

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

    /** Ключевые умения, концепты и законы домена на месте. */
    @Test
    void keySkillsConceptsAndLawsAreDefined() {
        // Act.
        var precedence = domain().getSkill(PRECEDENCE_SKILL);
        var arithmetics = domain().getConcept(ARITHMETICS_CONCEPT);
        var binaryPlus = domain().getConcept(BINARY_PLUS_CONCEPT);
        var law = domain().getNegativeLaw(HIGHER_PRECEDENCE_LAW);

        // Assert.
        assertNotNull(precedence);
        assertTrue(precedence.hasFlag(Skill.FLAG_VISIBLE_TO_TEACHER));
        assertTrue(precedence.getBitmask() != 0);
        assertNotNull(arithmetics);
        assertNotNull(binaryPlus);
        assertTrue(arithmetics.hasFlag(Concept.FLAG_VISIBLE_TO_TEACHER));
        assertTrue(binaryPlus.hasBaseConcept(arithmetics));
        assertTrue(binaryPlus.getBitmask() != 0);
        assertEquals(List.of(binaryPlus), domain().conceptsFromBitmask(binaryPlus.getBitmask()));
        assertNotNull(law);
        assertTrue(law.getBitmask() != 0);
        assertFalse(domain().getPositiveLaws().isEmpty());
    }

    /** Теги: языки программирования. */
    @Test
    void tagsCoverProgrammingLanguages() {
        // Act & Assert.
        assertTrue(domain().getTags().keySet().containsAll(Set.of(CPP_TAG, "Python", "Java")));
    }

    /** Доп. вопросы предлагаются по умениям, но не после подсказки. */
    @Test
    void supplementaryQuestionsAreOfferedForSkillsOnly() {
        // Act & Assert.
        assertTrue(domain().needSupplementaryQuestion(PRECEDENCE_SKILL, InteractionType.SEND_RESPONSE));
        assertFalse(domain().needSupplementaryQuestion(PRECEDENCE_SKILL, InteractionType.REQUEST_CORRECT_ANSWER));
        assertFalse(domain().needSupplementaryQuestion("stillUnevaluatedLeft", InteractionType.SEND_RESPONSE));
    }
}
