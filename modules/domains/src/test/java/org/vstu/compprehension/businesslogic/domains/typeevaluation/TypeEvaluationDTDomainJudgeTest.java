package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.vstu.compprehension.businesslogic.Explanation;
import org.vstu.compprehension.businesslogic.domains.TypeEvaluationDTDomain;
import org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.BankQuestion;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.enums.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.AVERAGE_OF_GRADES;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.BANK;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.EMPTY_NAME_OR_NAMES;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.FIRST_CHAR_PLUS_ONE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.answer;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.domain;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.judge;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.question;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.solution;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.withCorrectAnswers;

class TypeEvaluationDTDomainJudgeTest {

    static Stream<BankQuestion> bank() {
        return BANK.stream();
    }

    /** Эталонные типы, выбранные в порядке вычисления, принимаются шаг за шагом до конца. */
    @ParameterizedTest
    @MethodSource("bank")
    void referenceTypesAreAcceptedInEvaluationOrder(BankQuestion bankQuestion) {
        // Arrange.
        var question = question(bankQuestion);
        int steps = bankQuestion.solution().size();

        for (int step = 1; step <= steps; step++) {
            // Act.
            var result = judge(question, solution(question, bankQuestion, step));

            // Assert.
            assertTrue(result.isAnswerCorrect, bankQuestion.file() + " на шаге " + step);
            assertEquals(List.of(), result.violations);
            assertEquals(steps - step, result.IterationsLeft);
            assertFalse(result.domainSkills.isEmpty());
        }
    }

    /** Тип части выражения, чьи операнды ещё не определены, не оценивается и считается нарушением порядка. */
    @Test
    void answerBeforeOperandsIsEvaluationOrderViolation() {
        // Arrange.
        var question = question(AVERAGE_OF_GRADES);

        // Act.
        var result = judge(question, List.of(answer(question, "op_div", "t_float")));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(List.of(TypeEvaluationDTDomain.EVALUATION_ORDER_VIOLATION), lawNames(result.violations));
        assertEquals(List.of(), result.domainSkills);
        assertEquals(List.of(domain().getMessage("operands_first", Language.RUSSIAN)), messages(result.explanation));
        assertEquals(2, result.IterationsLeft);
    }

    /** Ответ int на деление целых объясняется двумя гипотезами, и студент видит оба объяснения. */
    @Test
    void integerForTrueDivisionShowsBothHypotheses() {
        // Arrange.
        var question = question(AVERAGE_OF_GRADES);
        var responses = new ArrayList<AnswerData>(solution(question, AVERAGE_OF_GRADES, 1));
        responses.add(answer(question, "op_div", "t_int"));

        // Act.
        var result = judge(question, responses);

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(List.of("true_division_result"), lawNames(result.violations));
        assertEquals(2, messages(result.explanation).size(), messages(result.explanation).toString());
        assertEquals(1, result.IterationsLeft);
    }

    /** Тип левого операнда у or с ложным левым объясняется неверной истинностью и перепутанными and/or. */
    @Test
    void leftOperandTypeForFalsyOrShowsBothHypotheses() {
        // Arrange.
        var question = question(EMPTY_NAME_OR_NAMES);

        // Act.
        var result = judge(question, List.of(answer(question, "op_or", "t_str")));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(List.of("logical_returned_operand"), lawNames(result.violations));
        assertEquals(2, messages(result.explanation).size(), messages(result.explanation).toString());
    }

    /** Верный ответ, к которому ведёт и ошибочное рассуждение, засчитывается без объяснений ошибок. */
    @Test
    void correctAnswerAlsoReachedByMisconceptionHasNoErrorExplanation() {
        // Arrange.
        var question = question(FIRST_CHAR_PLUS_ONE);

        // Act.
        var result = judge(question, List.of(answer(question, "op_first", "t_str")));

        // Assert.
        assertTrue(result.isAnswerCorrect);
        assertEquals(List.of(), result.violations);
        assertEquals(List.of(), messages(result.explanation));
    }

    /** Подсказка называет первую часть выражения, готовую к ответу, её эталонный тип и объясняет правило. */
    @Test
    void hintGivesFirstReadyOperationWithRule() {
        // Arrange.
        var question = question(AVERAGE_OF_GRADES);

        // Act.
        var hint = domain().getAnyNextCorrectAnswer(question, Language.RUSSIAN);

        // Assert.
        var answer = hint.answers.getFirst();
        assertEquals("op_len", answer.left().getDomainInfo());
        assertEquals("t_int", answer.right().getDomainInfo());
        assertFalse(messages(hint.explanation).isEmpty());
        assertTrue(hint.skillName.contains("length_applicability"), hint.skillName.toString());
    }

    /** После верного ответа на операнд подсказка переходит к следующей части выражения. */
    @Test
    void hintAfterSolvedOperandMovesToParent() {
        // Arrange.
        var question = withCorrectAnswers(question(AVERAGE_OF_GRADES), solution(question(AVERAGE_OF_GRADES), AVERAGE_OF_GRADES, 1), 1);

        // Act.
        var hint = domain().getAnyNextCorrectAnswer(question, Language.RUSSIAN);

        // Assert.
        var answer = hint.answers.getFirst();
        assertEquals("op_div", answer.left().getDomainInfo());
        assertEquals("t_float", answer.right().getDomainInfo());
        assertTrue(hint.skillName.contains("true_division_result"), hint.skillName.toString());
    }

    private static List<String> lawNames(List<ViolationData> violations) {
        return violations.stream().map(ViolationData::getLawName).distinct().toList();
    }

    private static List<String> messages(Explanation explanation) {
        var parts = explanation.getRawMessage().isEmpty() ? explanation.getChildren() : List.of(explanation);
        return parts.stream().map(part -> part.toHyperText(Language.RUSSIAN).getText()).toList();
    }
}
