package org.vstu.compprehension.businesslogic.domains.objectsscope;

import org.junit.jupiter.api.Test;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.enums.Language;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.answers;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.verdict;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.violations;
import static org.vstu.compprehension.businesslogic.domains.objectsscope.ObjectsScopeDomainFixture.LIFE_TIME_GLOBAL_VARIABLE;
import static org.vstu.compprehension.businesslogic.domains.objectsscope.ObjectsScopeDomainFixture.answer;
import static org.vstu.compprehension.businesslogic.domains.objectsscope.ObjectsScopeDomainFixture.bankQuestion;
import static org.vstu.compprehension.businesslogic.domains.objectsscope.ObjectsScopeDomainFixture.domain;

class ObjectsScopeDTDomainJudgeTest {

    /**
     * Конец трассы, отмеченный раньше строк, где жива глобальная переменная, — ошибка: переменная существует всё время
     * выполнения программы, даже если она не статическая.
     */
    @Test
    void endBeforeGlobalVariableStepsIsExplainedByGlobalLifetime() {
        // Arrange.
        var question = bankQuestion(LIFE_TIME_GLOBAL_VARIABLE);

        // Act.
        var judgement = domain().judgeAnswer(question, answers(answer(question, "EndObject")), List.of(), Language.RUSSIAN);

        // Assert.
        assertFalse(judgement.isAnswerCorrect());
        assertEquals(List.of("incorrectSteps"), violations(judgement).stream().map(ViolationData::getLawName).toList());
        var explanation = verdict(judgement).explanation().toHyperText(Language.RUSSIAN).getText();
        assertTrue(explanation.contains("\"b\" существует, так как является глобальной"), explanation);
    }
}
