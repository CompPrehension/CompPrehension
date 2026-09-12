package org.vstu.compprehension.businesslogic.domains.controlflowdt;

import org.junit.jupiter.api.Test;
import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.QuestionRequest;
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
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.domain;

class ControlFlowDTDomainStructureTest extends DomainStructureContract {

    private static final String CONDITION_SKILL = "required_condition_value_determined";
    private static final String DEBUG_SKILL = "unknown_incorrect";
    private static final String LOOPS_CONCEPT = "loops";
    private static final String WHILE_CONCEPT = "while_loop";
    private static final String RANGE_FOR_CONCEPT = "range_for_loop";
    private static final String CLASS_CONCEPT = "class";

    @Override
    protected DomainBase domain() {
        return ControlFlowDtDomainFixture.domain();
    }

    /** Идентификаторы домена. */
    @Test
    void domainIdentity() {
        // Act & Assert.
        assertEquals("ControlFlowDTDomain", domain().getDomainId());
        assertEquals("ctrl_flow_dt25", domain().getShortName());
        assertEquals("ctrl_flow_dt25", domain().getShortnameForQuestionSearch());
        assertEquals("DTReasoner", domain().getBackendId());
        assertFalse(domain().requiresSolving());
    }

    /** Умения преподавателю видны, отладочные — нет. */
    @Test
    void teacherSeesLearningSkillsButNotDebugOnes() {
        // Act.
        var teacherSkills = domain().getSkillSimplifiedHierarchy(Skill.FLAG_VISIBLE_TO_TEACHER).keySet();
        var condition = domain().getSkill(CONDITION_SKILL);
        var debug = domain().getSkill(DEBUG_SKILL);

        // Assert.
        assertEquals(20, teacherSkills.size());
        assertNotNull(condition);
        assertTrue(teacherSkills.contains(condition));
        assertNotNull(debug);
        assertFalse(teacherSkills.contains(debug));
        assertTrue(debug.getBitmask() != 0);
    }

    /** Концепты образуют иерархию конструкций языка. */
    @Test
    void conceptsFormLanguageConstructHierarchy() {
        // Act.
        var loops = domain().getConcept(LOOPS_CONCEPT);
        var whileLoop = domain().getConcept(WHILE_CONCEPT);
        var rangeFor = domain().getConcept(RANGE_FOR_CONCEPT);
        var cls = domain().getConcept(CLASS_CONCEPT);
        var loopChildren = domain().getChildrenOfConcept(LOOPS_CONCEPT);

        // Assert.
        assertNotNull(loops);
        assertEquals(0, loops.getBitmask());
        assertFalse(loops.hasFlag(Concept.FLAG_VISIBLE_TO_TEACHER));
        assertNotNull(whileLoop);
        assertTrue(whileLoop.hasFlag(Concept.FLAG_VISIBLE_TO_TEACHER));
        assertTrue(whileLoop.hasFlag(Concept.FLAG_TARGET_ENABLED));
        assertTrue(loopChildren.contains(whileLoop));
        assertTrue(loopChildren.contains(rangeFor));
        assertNotNull(cls);
        assertFalse(cls.hasFlag(Concept.FLAG_VISIBLE_TO_TEACHER));
        assertTrue(cls.hasFlag(Concept.FLAG_TARGET_ENABLED));
    }

    /** Законов у домена нет. */
    @Test
    void domainHasNoLaws() {
        // Act & Assert.
        assertTrue(domain().getNegativeLaws().isEmpty());
        assertTrue(domain().getPositiveLaws().isEmpty());
        assertTrue(domain().getQuestionNegativeLaws("OrderActs", List.of()).isEmpty());
        assertTrue(domain().getQuestionPositiveLaws("OrderActs", List.of()).isEmpty());
    }

    /** Теги: языки программирования. */
    @Test
    void tagsCoverProgrammingLanguages() {
        // Act & Assert.
        assertEquals(Set.of("C++", "Java", "Python"), domain().getTags().keySet());
    }

    /** Доп. вопросов домен не предлагает. */
    @Test
    void supplementaryQuestionsAreNotOffered() {
        // Act & Assert.
        assertFalse(domain().needSupplementaryQuestion(CONDITION_SKILL, InteractionType.SEND_RESPONSE));
        assertFalse(domain().needSupplementaryQuestion(CONDITION_SKILL, InteractionType.REQUEST_CORRECT_ANSWER));
    }

    /** Запрос вопроса ограничивается допустимым числом шагов. */
    @Test
    void questionRequestIsLimitedBySteps() {
        // Arrange.
        var request = QuestionRequest.builder().stepsMin(0).stepsMax(100).build();

        // Act.
        var valid = domain().ensureQuestionRequestValid(request);

        // Assert.
        assertEquals(2, valid.getStepsMin());
        assertEquals(23, valid.getStepsMax());
    }
}
