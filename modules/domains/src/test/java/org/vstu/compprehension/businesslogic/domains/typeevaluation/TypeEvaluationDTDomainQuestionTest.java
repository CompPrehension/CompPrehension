package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.AVERAGE_OF_GRADES;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.question;

class TypeEvaluationDTDomainQuestionTest {

    /** Вопрос приходит вместе со стилями домена: фронтенд оформления выражения с дугами не знает. */
    @Test
    void questionTextCarriesDomainStyles() {
        // Act.
        var text = question(AVERAGE_OF_GRADES).getContent().getQuestionText();

        // Assert.
        assertTrue(text.startsWith("<style>"), text.substring(0, Math.min(80, text.length())));
        assertTrue(text.contains(".comp-ph-typed-expr .comp-ph-expr-part::after"));
        assertTrue(text.contains("class=\"comp-ph-typed-expr\""));
    }
}
