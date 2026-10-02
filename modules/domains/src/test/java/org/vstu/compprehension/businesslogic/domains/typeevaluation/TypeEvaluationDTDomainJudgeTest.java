package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.vstu.compprehension.businesslogic.Explanation;
import org.vstu.compprehension.businesslogic.domains.TypeEvaluationDTDomain;
import org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.BankQuestion;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.AnswerHypothesisData;
import org.vstu.compprehension.data.question.HypothesisClarificationData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.enums.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.AVERAGE_OF_GRADES;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.BANK;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.EMPTY_NAME_OR_NAMES;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.FIRST_CHAR_PLUS_ONE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.GRADES_PLUS_ONE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.GRADE_COUNT;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.STUDENT_FIRST_GRADE;
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

    /**
     * Ответ int на деление целых объясняется двумя заблуждениями: какое из них у студента, неизвестно,
     * поэтому до его выбора он видит только, что тип неверен, а обе гипотезы остаются в результате.
     */
    @Test
    void integerForTrueDivisionIsAmbiguousAndShowsOnlyErrorStatement() {
        // Arrange.
        var question = question(AVERAGE_OF_GRADES);
        var responses = new ArrayList<AnswerData>(solution(question, AVERAGE_OF_GRADES, 1));
        responses.add(answer(question, "op_div", "t_int"));

        // Act.
        var result = judge(question, responses);

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(Set.of(new AnswerHypothesisData("operand_type", false),
                new AnswerHypothesisData("c_style_division", false)), Set.copyOf(result.hypotheses));
        assertEquals(List.of("true_division_result"), lawNames(result.violations));
        assertEquals(List.of("Выражение <code>total / len(grades)</code> не может иметь тип <code>int</code>."), messages(result.explanation));
        assertEquals(1, result.IterationsLeft);
    }

    /**
     * Тип левого операнда у or с ложным левым объясняется неверной истинностью, перепутанными and/or и тем, что правый
     * операнд names прочитан как похожая name: до выбора причины студент видит только, что тип неверен, а не одно из трёх объяснений.
     */
    @Test
    void leftOperandTypeForFalsyOrIsAmbiguousAndShowsOnlyErrorStatement() {
        // Arrange.
        var question = question(EMPTY_NAME_OR_NAMES);

        // Act.
        var result = judge(question, List.of(answer(question, "op_or", "t_str")));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(Set.of(new AnswerHypothesisData("truthiness_misjudged", false),
                new AnswerHypothesisData("and_or_confused", false),
                new AnswerHypothesisData("variable_confused", false)), Set.copyOf(result.hypotheses));
        assertEquals(Set.of("logical_returned_operand", "operand_identification"), Set.copyOf(lawNames(result.violations)));
        assertEquals(List.of("Выражение <code>name or names</code> не может иметь тип <code>str</code>."), messages(result.explanation));
    }

    /** Ошибку, которую объясняет единственное заблуждение, студенту объясняет именно оно. */
    @Test
    void errorWithSingleHypothesisShowsItsExplanation() {
        // Arrange.
        var question = question(FIRST_CHAR_PLUS_ONE);

        // Act.
        var result = judge(question, List.of(answer(question, "op_first", "t_int")));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(List.of(new AnswerHypothesisData("index_type", false)), result.hypotheses);
        assertEquals(List.of("Выражение <code>line[0]</code> не может иметь тип <code>int</code>, потому что индекс лишь указывает"
                + " позицию, а результат обращения — сам элемент последовательности."), messages(result.explanation));
        assertNull(result.clarification);
    }

    /** Объяснение называет часть выражения так, как она написана в коде, — с кавычками и скобками. */
    @Test
    void explanationNamesExpressionPartAsWrittenInCode() {
        // Arrange.
        var question = question(STUDENT_FIRST_GRADE);

        // Act.
        var result = judge(question, List.of(answer(question, "op_key", "t_str")));

        // Assert.
        assertEquals(List.of("Выражение <code>student[\"grades\"]</code> не может иметь тип <code>str</code>, потому что по ключу"
                + " из словаря возвращается хранящееся под ним значение, а не сам ключ."), messages(result.explanation));
    }

    /** Ответ, который не объясняет ни одно из известных рассуждений, остаётся ошибкой без гипотез. */
    @Test
    void unexplainedErrorHasNoHypotheses() {
        // Arrange.
        var question = question(AVERAGE_OF_GRADES);

        // Act.
        var result = judge(question, List.of(answer(question, "op_len", "t_float")));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(List.of(), result.hypotheses);
        assertEquals(List.of("length_applicability"), lawNames(result.violations));
    }

    /**
     * Верный ответ, к которому ведёт и ошибочное рассуждение, засчитывается без объяснений ошибок,
     * но сохраняет гипотезу-заблуждение: такие ответы не подтверждают знание правила.
     */
    @Test
    void correctAnswerAlsoReachedByMisconceptionKeepsMisconceptionHypothesis() {
        // Arrange.
        var question = question(FIRST_CHAR_PLUS_ONE);

        // Act.
        var result = judge(question, List.of(answer(question, "op_first", "t_str")));

        // Assert.
        assertTrue(result.isAnswerCorrect);
        assertEquals(Set.of(new AnswerHypothesisData("rule", true),
                new AnswerHypothesisData("container_type", false),
                new AnswerHypothesisData("nesting_level_skipped", false)), Set.copyOf(result.hypotheses));
        assertEquals(List.of(), result.violations);
        assertEquals(List.of(), messages(result.explanation));
        assertNull(result.clarification);
    }

    /** Студента, ошибку которого объясняют два заблуждения, спрашивают, почему он выбрал этот тип. */
    @Test
    void ambiguousErrorAsksWhyTypeWasChosen() {
        // Arrange.
        var question = question(AVERAGE_OF_GRADES);
        var responses = new ArrayList<AnswerData>(solution(question, AVERAGE_OF_GRADES, 1));
        responses.add(answer(question, "op_div", "t_int"));

        // Act.
        var result = judge(question, responses);

        // Assert.
        assertNotNull(result.clarification);
        assertEquals("Почему вы выбрали тип int?", result.clarification.prompt());
        assertEquals(Set.of(
                new HypothesisClarificationData.Option("operand_type",
                        "Результат берёт тип одного из операндов.",
                        "Выражение <code>total / len(grades)</code> не может иметь тип <code>int</code>, потому что оператор <code>/</code> всегда"
                                + " возвращает вещественный результат."),
                new HypothesisClarificationData.Option("c_style_division",
                        "Деление целых чисел даёт целое число.",
                        "Выражение <code>total / len(grades)</code> не может иметь тип <code>int</code>, потому что оператор <code>/</code> всегда возвращает"
                                + " вещественный результат.")),
                Set.copyOf(result.clarification.options()));
    }

    /** Причина «неверная истинность» называет ту истинность левого операнда, которую студент ему приписал. */
    @Test
    void truthinessReasonNamesTruthinessStudentAssumed() {
        // Arrange.
        var question = question(EMPTY_NAME_OR_NAMES);

        // Act.
        var result = judge(question, List.of(answer(question, "op_or", "t_str")));

        // Assert.
        assertNotNull(result.clarification);
        var reasons = result.clarification.options().stream()
                .collect(Collectors.toMap(HypothesisClarificationData.Option::hypothesis,
                        HypothesisClarificationData.Option::reason));
        assertEquals("Левый операнд истинный.", reasons.get("truthiness_misjudged"));
        assertEquals("Возвращается первый ложный операнд.", reasons.get("and_or_confused"));
    }

    /**
     * Похожая переменная может стоять в том же выражении, что и спутанная с ней, поэтому причина называет, какой операнд
     * студент прочитал не так.
     */
    @Test
    void lookalikeReasonNamesOperandWhenBothVariablesAreInExpression() {
        // Arrange.
        var question = question(EMPTY_NAME_OR_NAMES);

        // Act.
        var result = judge(question, List.of(answer(question, "op_or", "t_str")));

        // Assert.
        assertNotNull(result.clarification);
        var option = result.clarification.options().stream()
                .filter(o -> o.hypothesis().equals("variable_confused"))
                .findFirst().orElseThrow();
        assertEquals("Мне показалось, что правый операнд — <code>name</code>, а не <code>names</code>.", option.reason());
        assertEquals("Выражение <code>name or names</code> не может иметь тип <code>str</code>, потому что правый"
                + " операнд — <code>names</code>, а не <code>name</code>.", option.explanation());
    }

    /** Ошибку, которая верна для похожей переменной из условия, объясняют тем, что в выражении стоит другая переменная. */
    @Test
    void errorForLookalikeVariableNamesTheVariableInExpression() {
        // Arrange.
        var question = question(GRADE_COUNT);

        // Act.
        var result = judge(question, List.of(answer(question, "op_len", "t_error")));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(List.of(new AnswerHypothesisData("variable_confused", false)), result.hypotheses);
        assertEquals(List.of("operand_identification"), lawNames(result.violations));
        assertEquals(List.of("Выражение <code>len(grades)</code> не может иметь тип <code>TypeError</code>, потому что в выражении"
                + " стоит <code>grades</code>, а не <code>grade</code>."), messages(result.explanation));
        assertNull(result.clarification);
    }

    /**
     * Ответ, который объясняют и заблуждение, и прочтение похожей переменной, получает только утверждение, что тип неверен,
     * уточняющий вопрос с обеими причинами и нарушения обоих навыков.
     */
    @Test
    void errorExplainedByMisconceptionAndReadingAsksWhichOne() {
        // Arrange.
        var question = question(GRADES_PLUS_ONE);

        // Act.
        var result = judge(question, List.of(answer(question, "op_add", "t_int")));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(Set.of(new AnswerHypothesisData("operand_type", false),
                new AnswerHypothesisData("variable_confused", false)), Set.copyOf(result.hypotheses));
        assertEquals(Set.of("sequence_operation_applicability", "operand_identification"),
                Set.copyOf(lawNames(result.violations)));
        assertEquals(List.of("Выражение <code>grades + 1</code> не может иметь тип <code>int</code>."), messages(result.explanation));
        assertNotNull(result.clarification);
        assertEquals(Set.of("Результат берёт тип одного из операндов.", "Мне показалось, что левый операнд — <code>grade</code>, а не <code>grades</code>."),
                result.clarification.options().stream()
                        .map(HypothesisClarificationData.Option::reason)
                        .collect(Collectors.toSet()));
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
