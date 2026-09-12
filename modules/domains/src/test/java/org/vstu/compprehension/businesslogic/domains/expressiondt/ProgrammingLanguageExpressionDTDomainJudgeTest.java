package org.vstu.compprehension.businesslogic.domains.expressiondt;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.vstu.compprehension.businesslogic.Tag;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures;
import org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.BankQuestion;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.enums.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.ASSIGN_UNARY_MINUS_PLUS;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.BANK;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.END_TOKEN;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.MEMBER_ACCESS_PLUS;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.MUL_PLUS_MINUS;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.PARENTHESES_AND_UNARY_MINUS;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.bankQuestion;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.domain;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.endToken;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.operator;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.operatorsInOrder;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.interaction;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.responses;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.violation;

class ProgrammingLanguageExpressionDTDomainJudgeTest {

    private static final String EARLY_FINISH_VIOLATION = "stillUnevaluatedLeft";
    private static final String EARLY_FINISH_SKILL = "earlyfinish_strict_order_operators_present";
    private static final String LEFT_OPERATOR_HAS_PRIORITY = "right_competing_to_left_precedence";
    private static final String RIGHT_OPERATOR_HAS_PRIORITY = "left_competing_to_right_precedence";
    private static final String LEFT_OPERATOR_BY_ASSOCIATIVITY = "right_competing_to_left_associativity";

    static Stream<BankQuestion> bank() {
        return BANK.stream();
    }

    // ---- judgeQuestion ----

    /** Верная последовательность принимается шаг за шагом до конца. */
    @ParameterizedTest
    @MethodSource("bank")
    void judgeQuestionAcceptsEvaluationOrderStepByStep(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);
        var given = new ArrayList<AnswerObjectData>();

        for (var operator : operatorsInOrder(question, bankQuestion)) {
            given.add(operator);

            // Act.
            var result = judge(question, given);

            // Assert.
            assertTrue(result.isAnswerCorrect, bankQuestion.expression() + " на шаге " + operator.getHyperText());
            assertEquals(List.of(), result.violations);
            assertEquals(bankQuestion.steps() - given.size(), result.IterationsLeft);
            assertFalse(result.domainSkills.isEmpty());
        }
    }

    /** После всех операторов «всё вычислено» принимается. */
    @ParameterizedTest
    @MethodSource("bank")
    void judgeQuestionAcceptsFinishAfterEveryOperator(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);
        var given = new ArrayList<>(operatorsInOrder(question, bankQuestion));
        given.add(endToken(question));

        // Act.
        var result = judge(question, given);

        // Assert.
        assertTrue(result.isAnswerCorrect);
        assertEquals(List.of(), result.violations);
        assertEquals(0, result.IterationsLeft);
    }

    /** Преждевременное «всё вычислено» отклоняется. */
    @ParameterizedTest
    @MethodSource("bank")
    void judgeQuestionRejectsEarlyFinish(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);

        // Act.
        var result = judge(question, List.of(endToken(question)));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(bankQuestion.steps(), result.IterationsLeft);
        assertTrue(lawNames(result.violations).contains(EARLY_FINISH_VIOLATION));
        assertTrue(lawNames(result.violations).contains(EARLY_FINISH_SKILL));
        assertFalse(result.explanation.toHyperText(Language.ENGLISH).getText().isBlank());
    }

    /** Второй по порядку оператор первым не принимается. */
    @ParameterizedTest
    @MethodSource("bank")
    void judgeQuestionRejectsSecondOperatorFirst(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);
        var second = operatorsInOrder(question, bankQuestion).get(1);

        // Act.
        var result = judge(question, List.of(second));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(bankQuestion.steps(), result.IterationsLeft);
        assertFalse(result.violations.isEmpty());
        assertFalse(result.explanation.toHyperText(Language.ENGLISH).getText().isBlank());
    }

    /** Оператор слева с большим приоритетом: `->` раньше `+`. */
    @Test
    void judgeQuestionNamesLeftOperatorWithHigherPrecedence() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);

        // Act.
        var result = judge(question, List.of(operator(question, "+")));

        // Assert.
        assertEquals(List.of(LEFT_OPERATOR_HAS_PRIORITY), lawNames(result.violations));
        assertTrue(result.domainSkills.contains(LEFT_OPERATOR_HAS_PRIORITY));
    }

    /** Оператор справа с большим приоритетом: унарный `-` раньше `&`. */
    @Test
    void judgeQuestionNamesRightOperatorWithHigherPrecedence() {
        // Arrange.
        var question = bankQuestion(PARENTHESES_AND_UNARY_MINUS);

        // Act.
        var result = judge(question, List.of(operator(question, "&")));

        // Assert.
        assertEquals(List.of(RIGHT_OPERATOR_HAS_PRIORITY), lawNames(result.violations));
    }

    /** Левая ассоциативность: `+` раньше `-` при равном приоритете. */
    @Test
    void judgeQuestionNamesAssociativityMistake() {
        // Arrange.
        var question = bankQuestion(MUL_PLUS_MINUS);

        // Act.
        var result = judge(question, List.of(operator(question, "*"), operator(question, "-")));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(List.of(LEFT_OPERATOR_BY_ASSOCIATIVITY), lawNames(result.violations));
        assertEquals(MUL_PLUS_MINUS.steps() - 1, result.IterationsLeft);
    }

    /** Присваивание раньше своего правого операнда — ошибка. */
    @Test
    void judgeQuestionRejectsAssignmentBeforeItsOperand() {
        // Arrange.
        var question = bankQuestion(ASSIGN_UNARY_MINUS_PLUS);

        // Act.
        var result = judge(question, List.of(operator(question, "=")));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(List.of(RIGHT_OPERATOR_HAS_PRIORITY), lawNames(result.violations));
    }

    /** Объяснение ошибки локализовано. */
    @Test
    void judgeQuestionExplainsMistakeInRequestedLanguage() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var tags = domain().resolveTags(question.getContent().getTags());
        var wrong = responses(operator(question, "+"));

        // Act.
        var english = domain().judgeQuestion(question, wrong, tags, Language.ENGLISH).explanation.toHyperText(Language.ENGLISH).getText();
        var russian = domain().judgeQuestion(question, wrong, tags, Language.RUSSIAN).explanation.toHyperText(Language.RUSSIAN).getText();

        // Assert.
        assertTrue(english.contains("cannot be evaluated yet"));
        assertFalse(russian.isBlank());
        assertNotEquals(english, russian);
    }

    // ---- getAnyNextCorrectAnswer ----

    /** Подсказки повторяют порядок вычисления, затем «всё вычислено». */
    @ParameterizedTest
    @MethodSource("bank")
    void hintsFollowEvaluationOrderAndEndWithFinish(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);
        var given = new ArrayList<AnswerObjectData>();

        for (var expected : operatorsInOrder(question, bankQuestion)) {
            // Act.
            var hint = domain().getAnyNextCorrectAnswer(withCorrectSteps(question, given, bankQuestion), Language.ENGLISH);

            // Assert.
            assertEquals(1, hint.answers.size());
            assertEquals(expected.getHyperText(), hint.answers.getFirst().getLeft().getHyperText());
            assertEquals(hint.answers.getFirst().getLeft(), hint.answers.getFirst().getRight());
            assertFalse(hint.skillName.isEmpty());
            assertNull(hint.lawName);
            assertFalse(hint.explanation.getChildren().isEmpty());
            given.add(expected);
        }
        var finish = domain().getAnyNextCorrectAnswer(withCorrectSteps(question, given, bankQuestion), Language.ENGLISH);
        assertEquals(END_TOKEN, finish.answers.getFirst().getLeft().getDomainInfo());
    }

    /** Подсказка объясняет выбор на нужном языке. */
    @Test
    void hintExplainsChoiceInRequestedLanguage() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);

        // Act.
        var english = domain().getAnyNextCorrectAnswer(question, Language.ENGLISH).explanation.toHyperText(Language.ENGLISH).getText();
        var russian = domain().getAnyNextCorrectAnswer(question, Language.RUSSIAN).explanation.toHyperText(Language.RUSSIAN).getText();

        // Assert.
        assertTrue(english.contains("can be evaluated"));
        assertFalse(russian.isBlank());
        assertNotEquals(english, russian);
    }

    /** Ошибочная попытка на подсказку не влияет. */
    @Test
    void hintIgnoresMistakenInteraction() {
        // Arrange.
        var question = bankQuestion(PARENTHESES_AND_UNARY_MINUS);
        var mistaken = question.withInteraction(interaction(1L, List.of(operator(question, "&")),
                List.of(violation(RIGHT_OPERATOR_HAS_PRIORITY)), PARENTHESES_AND_UNARY_MINUS.steps()));

        // Act.
        var hint = domain().getAnyNextCorrectAnswer(mistaken, Language.ENGLISH);

        // Assert.
        assertEquals("-", hint.answers.getFirst().getLeft().getHyperText());
    }

    // ---- getFullSolutionTrace ----

    /** Трасса решения перечисляет вычисленные операторы по порядку. */
    @ParameterizedTest
    @MethodSource("bank")
    void solutionTraceListsEvaluatedOperatorsInOrder(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);
        var solved = withCorrectSteps(question, operatorsInOrder(question, bankQuestion), bankQuestion);

        // Act.
        var trace = domain().getFullSolutionTrace(solved, Language.ENGLISH);

        // Assert.
        assertEquals(bankQuestion.steps(), trace.size());
        for (int step = 0; step < bankQuestion.steps(); step++) {
            var line = trace.get(step).getText();
            assertTrue(line.contains(bankQuestion.evaluationOrder().get(step)), line);
            assertTrue(line.contains("was calculated"), line);
        }
    }

    /** Без взаимодействий трасса пуста. */
    @Test
    void solutionTraceIsEmptyBeforeFirstAnswer() {
        // Act & Assert.
        assertTrue(domain().getFullSolutionTrace(bankQuestion(MEMBER_ACCESS_PLUS), Language.ENGLISH).isEmpty());
    }

    /** Последняя ошибка попадает в трассу. */
    @Test
    void solutionTraceIncludesLastMistake() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var arrow = operator(question, "->");
        var plus = operator(question, "+");
        var afterMistake = question
                .withInteraction(interaction(1L, List.of(plus), List.of(violation(LEFT_OPERATOR_HAS_PRIORITY)), 2));

        // Act.
        var trace = domain().getFullSolutionTrace(afterMistake, Language.ENGLISH);

        // Assert.
        assertEquals(1, trace.size());
        assertTrue(trace.getFirst().getText().contains("+"));
        assertTrue(trace.getFirst().getText().contains("#ff9"));
        assertFalse(domain().getFullSolutionTrace(
                question.withInteraction(interaction(2L, List.of(arrow), List.of(), 1)), Language.ENGLISH).getFirst().getText().contains("#ff9"));
    }

    /** Трасса локализована. */
    @Test
    void solutionTraceIsLocalized() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var solved = withCorrectSteps(question, operatorsInOrder(question, MEMBER_ACCESS_PLUS), MEMBER_ACCESS_PLUS);

        // Act.
        var english = domain().getFullSolutionTrace(solved, Language.ENGLISH).getFirst().getText();
        var russian = domain().getFullSolutionTrace(solved, Language.RUSSIAN).getFirst().getText();

        // Assert.
        assertNotEquals(english, russian);
        assertFalse(russian.isBlank());
    }

    // ---- вспомогательное ----

    private static Domain.InterpretSentenceResult judge(QuestionData question, List<AnswerObjectData> answers) {
        List<Tag> tags = domain().resolveTags(question.getContent().getTags());
        return domain().judgeQuestion(question, responses(answers), tags, Language.ENGLISH);
    }

    private static List<String> lawNames(List<ViolationData> violations) {
        return violations.stream().map(ViolationData::getLawName).toList();
    }

    private static QuestionData withCorrectSteps(QuestionData question, List<AnswerObjectData> given, BankQuestion bankQuestion) {
        return DomainFixtures.withCorrectSteps(question, given, bankQuestion.steps());
    }
}
