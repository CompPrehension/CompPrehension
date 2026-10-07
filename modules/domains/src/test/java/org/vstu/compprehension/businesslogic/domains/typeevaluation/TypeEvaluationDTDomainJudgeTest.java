package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.vstu.compprehension.businesslogic.Explanation;
import org.vstu.compprehension.businesslogic.domains.Judgement;
import org.vstu.compprehension.businesslogic.domains.Reasoning;
import org.vstu.compprehension.businesslogic.domains.TypeEvaluationDTDomain;
import org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.BankQuestion;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.Assumption;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.enums.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.appliedKnowledge;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.onlyReasoning;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.reasoned;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.verdict;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.violations;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.AVERAGE_OF_GRADES;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.BANK;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.COUNT_PLUS_TOTAL;
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

    /** Эталонные типы, выбранные в порядке вычисления, принимаются шаг за шагом до конца и засчитывают навыки. */
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
            assertTrue(result.isAnswerCorrect(), bankQuestion.file() + " на шаге " + step);
            assertEquals(List.of(), violations(result));
            assertEquals(steps - step, result.stepsLeft());
            assertFalse(appliedKnowledge(result).isEmpty(), bankQuestion.file() + " на шаге " + step);
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
        assertFalse(result.isAnswerCorrect());
        assertEquals(List.of(TypeEvaluationDTDomain.EVALUATION_ORDER_VIOLATION), lawNames(violations(result)));
        assertEquals(List.of(), appliedKnowledge(result));
        assertEquals(List.of(domain().getMessage("operands_first", Language.RUSSIAN)),
                messages(verdict(result).explanation()));
        assertEquals(2, result.stepsLeft());
    }

    /**
     * Ответ int на деление целых объясняется двумя заблуждениями одного навыка: какое из них у студента, неизвестно,
     * поэтому до его выбора ему говорят только, что тип неверен, а нарушение навыка засчитывается сразу.
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
        assertFalse(result.isAnswerCorrect());
        assertEquals(Set.of(new Hypothesis("operand_type", false), new Hypothesis("c_style_division", false)),
                hypotheses(result));
        assertEquals(List.of("true_division_result"), lawNames(violations(result)));
        assertEquals(List.of("Выражение <code>total / len(grades)</code> не может иметь тип <code>int</code>."),
                messages(reasoned(result).inquiry().statement()));
        assertEquals(1, result.stepsLeft());
    }

    /**
     * Тип левого операнда у or с ложным левым объясняется неверной истинностью, перепутанными and/or и тем, что правый
     * операнд names прочитан как похожая name. Навыки у этих причин разные, и пока студент не назвал свою, ни один из
     * них не засчитывается нарушенным.
     */
    @Test
    void leftOperandTypeForFalsyOrIsAmbiguousAndCountsNoSkillUntilReasonIsNamed() {
        // Arrange.
        var question = question(EMPTY_NAME_OR_NAMES);
        var responses = List.<AnswerData>of(answer(question, "op_or", "t_str"));

        // Act.
        var result = judge(question, responses);

        // Assert.
        assertFalse(result.isAnswerCorrect());
        assertEquals(Map.of(
                        "truthiness_misjudged", List.of("logical_returned_operand"),
                        "and_or_confused", List.of("logical_returned_operand"),
                        "variable_confused", List.of("operand_identification")),
                violationsByHypothesis(result));
        assertEquals(List.of(), violations(result));
        assertEquals(List.of("Выражение <code>name or names</code> не может иметь тип <code>str</code>."),
                messages(reasoned(result).inquiry().statement()));
    }

    /** Ошибку, которую объясняет единственное заблуждение, студенту объясняет именно оно. */
    @Test
    void errorWithSingleHypothesisShowsItsExplanation() {
        // Arrange.
        var question = question(FIRST_CHAR_PLUS_ONE);

        // Act.
        var result = judge(question, List.of(answer(question, "op_first", "t_int")));

        // Assert.
        assertFalse(result.isAnswerCorrect());
        assertEquals(Set.of(new Hypothesis("index_type", false)), hypotheses(result));
        assertEquals(List.of("Выражение <code>line[0]</code> не может иметь тип <code>int</code>, потому что индекс лишь указывает"
                + " позицию, а результат обращения — сам элемент последовательности."),
                messages(onlyReasoning(reasoned(result)).explanation()));
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
                + " из словаря возвращается хранящееся под ним значение, а не сам ключ."),
                messages(onlyReasoning(reasoned(result)).explanation()));
    }

    /**
     * Распознавание операндов засчитывается за верный ответ, только если было что путать: у len(grades) есть похожая
     * переменная grade, у len(grades) в задаче о среднем — нет.
     */
    @Test
    void operandIdentificationIsAppliedOnlyWhenOperandHasLookalike() {
        // Arrange.
        var withLookalike = question(GRADE_COUNT);
        var withoutLookalike = question(AVERAGE_OF_GRADES);

        // Act.
        var lookalikeResult = judge(withLookalike, solution(withLookalike, GRADE_COUNT, 1));
        var plainResult = judge(withoutLookalike, solution(withoutLookalike, AVERAGE_OF_GRADES, 1));

        // Assert.
        assertTrue(lookalikeResult.isAnswerCorrect());
        assertTrue(appliedKnowledge(lookalikeResult).contains("operand_identification"), appliedKnowledge(lookalikeResult).toString());
        assertTrue(plainResult.isAnswerCorrect());
        assertFalse(appliedKnowledge(plainResult).contains("operand_identification"), appliedKnowledge(plainResult).toString());
    }

    /** Ответ, который не объясняет ни одно из известных рассуждений, остаётся ошибкой без гипотез. */
    @Test
    void unexplainedErrorHasNoHypotheses() {
        // Arrange.
        var question = question(AVERAGE_OF_GRADES);

        // Act.
        var result = judge(question, List.of(answer(question, "op_len", "t_float")));

        // Assert.
        assertFalse(result.isAnswerCorrect());
        assertEquals(Set.of(), hypotheses(result));
        assertEquals(List.of("length_applicability"), lawNames(violations(result)));
    }

    /**
     * Ответ, который не объясняет ни одно рассуждение, объясняется только по выражению как написано: прочтения
     * с похожими переменными к нему не ведут, и их допущения студенту не показываются и не засчитываются.
     */
    @Test
    void unexplainedAnswerIsExplainedWithoutLookalikeReadings() {
        // Arrange.
        var question = question(COUNT_PLUS_TOTAL);

        // Act.
        var result = judge(question, List.of(answer(question, "op_add", "t_str")));

        // Assert.
        assertFalse(result.isAnswerCorrect());
        assertEquals(Set.of(), hypotheses(result));
        assertEquals(List.of("Выражение <code>count + total</code> не может иметь тип <code>str</code>, потому что ни у"
                        + " одного из операндов (<code>int</code>, <code>int</code>) нет такого типа, и оператор <code>+</code>"
                        + " его не создаёт."),
                messages(verdict(result).explanation()));
        assertEquals(List.of("numeric_result_type"), lawNames(violations(result)));
    }

    /**
     * Верный ответ, к которому ведёт и ошибочное рассуждение, засчитывается без нарушений,
     * но сохраняет гипотезу-заблуждение: такие ответы не подтверждают знание правила.
     */
    @Test
    void correctAnswerAlsoReachedByMisconceptionKeepsMisconceptionHypothesis() {
        // Arrange.
        var question = question(FIRST_CHAR_PLUS_ONE);

        // Act.
        var result = judge(question, List.of(answer(question, "op_first", "t_str")));

        // Assert.
        assertTrue(result.isAnswerCorrect());
        assertEquals(Set.of(new Hypothesis("rule", true), new Hypothesis("container_type", false),
                new Hypothesis("nesting_level_skipped", false)), hypotheses(result));
        assertEquals(List.of(), violations(result));
    }

    /** Ошибку, которую объясняют два заблуждения, можно уточнить: студента спрашивают, почему он выбрал тип, и предлагают обе причины. */
    @Test
    void ambiguousErrorAsksWhyTypeWasChosen() {
        // Arrange.
        var question = question(AVERAGE_OF_GRADES);
        var responses = new ArrayList<AnswerData>(solution(question, AVERAGE_OF_GRADES, 1));
        responses.add(answer(question, "op_div", "t_int"));

        // Act.
        var result = judge(question, responses);

        // Assert.
        assertEquals("Почему вы выбрали тип <code>int</code>?", reasoned(result).inquiry().prompt());
        assertEquals(Set.of(
                new Offered("operand_type",
                        "Результат берёт тип одного из операндов.",
                        "Выражение <code>total / len(grades)</code> не может иметь тип <code>int</code>, потому что оператор <code>/</code> всегда"
                                + " возвращает вещественный результат."),
                new Offered("c_style_division",
                        "Деление целых чисел даёт целое число.",
                        "Выражение <code>total / len(grades)</code> не может иметь тип <code>int</code>, потому что оператор <code>/</code> всегда возвращает"
                                + " вещественный результат.")),
                offered(result));
    }

    /**
     * Верный ответ, к которому ведут и правило, и заблуждения, можно уточнить: варианты — верное рассуждение
     * и каждое заблуждение, а выбравший заблуждение узнаёт, что ответ верен, но рассуждение ошибочно.
     */
    @Test
    void correctAnswerReachedByMisconceptionsOffersReasoningClarification() {
        // Arrange.
        var question = question(FIRST_CHAR_PLUS_ONE);
        var responses = List.<AnswerData>of(answer(question, "op_first", "t_str"));

        // Act.
        var result = judge(question, responses);

        // Assert.
        assertTrue(result.isAnswerCorrect());
        assertEquals("Почему вы выбрали тип <code>str</code>?", reasoned(result).inquiry().prompt());
        assertEquals(Set.of(
                new Offered("rule",
                        "Обращение по индексу берёт один элемент из <code>str</code>, а элементы там имеют тип <code>str</code>.",
                        "Выражение <code>line[0]</code> имеет тип <code>str</code>, потому что обращение по индексу берёт один"
                                + " элемент из <code>str</code>, а элементы там имеют тип <code>str</code>."),
                new Offered("container_type",
                        "Обращение по индексу даёт последовательность того же типа.",
                        "Выражение <code>line[0]</code> действительно имеет тип <code>str</code>, но рассуждение ошибочно:"
                                + " обращение по индексу берёт из последовательности один элемент, а не последовательность;"
                                + " часть последовательности даёт срез."),
                new Offered("nesting_level_skipped",
                        "Обращение по индексу доходит до самых внутренних элементов.",
                        "Выражение <code>line[0]</code> действительно имеет тип <code>str</code>, но рассуждение ошибочно:"
                                + " одно обращение по индексу достаёт элемент из <code>str</code>, а не элемент этого элемента.")),
                offered(result));
    }

    /**
     * Заблуждение «результат берёт тип операнда», приведшее к верному целому типу произведения целых чисел,
     * объясняется правилами арифметики, а не поведением логических значений.
     */
    @Test
    void operandTypeReasoningForIntegerProductExplainsArithmeticRule() {
        // Arrange.
        var question = question(GRADE_COUNT);
        var responses = new ArrayList<AnswerData>(solution(question, GRADE_COUNT, 1));
        responses.add(answer(question, "op_mul", "t_int"));

        // Act.
        var result = judge(question, responses);

        // Assert.
        assertTrue(result.isAnswerCorrect());
        assertEquals("Выражение <code>len(grades) * grade</code> действительно имеет тип <code>int</code>, но рассуждение"
                + " ошибочно: тип результата задают правила арифметики, а не тип операнда, и оператор <code>*</code>"
                + " над целыми операндами даёт целое число.",
                reasoning(result, "operand_type").explanation().getRawMessage().getText());
    }

    /** Верный ответ, к которому не ведёт ни одно заблуждение, уточнять нечего. */
    @Test
    void correctAnswerReachedOnlyByRuleHasNoReasoningClarification() {
        // Arrange.
        var question = question(AVERAGE_OF_GRADES);

        // Act.
        var result = judge(question, List.of(answer(question, "op_len", "t_int")));

        // Assert.
        assertTrue(result.isAnswerCorrect());
        assertEquals(Set.of(new Hypothesis("rule", true)), hypotheses(result));
    }

    /** Причина «неверная истинность» называет ту истинность левого операнда, которую студент ему приписал. */
    @Test
    void truthinessReasonNamesTruthinessStudentAssumed() {
        // Arrange.
        var question = question(EMPTY_NAME_OR_NAMES);

        // Act.
        var result = judge(question, List.of(answer(question, "op_or", "t_str")));

        // Assert.
        assertEquals("Левый операнд истинный.", reasoning(result, "truthiness_misjudged").reason());
        assertEquals("Возвращается первый ложный операнд.", reasoning(result, "and_or_confused").reason());
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
        var lookalike = reasoning(result, "variable_confused");
        assertEquals("Мне показалось, что правый операнд — <code>name</code>, а не <code>names</code>.", lookalike.reason());
        assertEquals("Выражение <code>name or names</code> не может иметь тип <code>str</code>, потому что правый"
                + " операнд — <code>names</code>, а не <code>name</code>.", lookalike.explanation().getRawMessage().getText());
    }

    /**
     * Если похожие переменные есть у обоих операндов, путаница каждого из них — своё рассуждение с той же гипотезой:
     * причины называют разные операнды, и о каждой можно спросить отдельно.
     */
    @Test
    void lookalikesOfBothOperandsAreSeparateReasonings() {
        // Arrange.
        var question = question(COUNT_PLUS_TOTAL);

        // Act.
        var result = judge(question, List.of(answer(question, "op_add", "t_error")));

        // Assert.
        assertFalse(result.isAnswerCorrect());
        assertEquals(Set.of(
                        "Мне показалось, что левый операнд — <code>counts</code>, а не <code>count</code>.",
                        "Мне показалось, что правый операнд — <code>totals</code>, а не <code>total</code>."),
                reasoned(result).reasonings().stream()
                        .filter(reasoning -> "variable_confused".equals(nameOf(reasoning)))
                        .map(Reasoning::reason)
                        .collect(Collectors.toSet()));
    }

    /**
     * Ответ list[int] на count + total объясняют только сочетания ошибок: студенту предлагают их как причины
     * из двух частей и объясняют обе ошибки.
     */
    @Test
    void answerExplainedByCombinedErrorsOffersBothParts() {
        // Arrange.
        var question = question(COUNT_PLUS_TOTAL);

        // Act.
        var result = judge(question, List.of(answer(question, "op_add", "t_list_int")));

        // Assert.
        assertFalse(result.isAnswerCorrect());
        assertEquals(Set.of(
                new Offered("variable_confused + operand_type",
                        "Мне показалось, что левый операнд — <code>counts</code>, а не <code>count</code>."
                                + " Результат берёт тип одного из операндов.",
                        "Выражение <code>count + total</code> не может иметь тип <code>list[int]</code>, потому что левый"
                                + " операнд — <code>count</code>, а не <code>counts</code>. Кроме того, оператор <code>+</code>"
                                + " нельзя применить к операндам типов <code>list[int]</code> и <code>int</code>, и ни один"
                                + " из операндов не может стать его результатом."),
                new Offered("variable_confused + element_appended",
                        "Мне показалось, что левый операнд — <code>counts</code>, а не <code>count</code>."
                                + " Оператор <code>+</code> добавляет элемент в конец списка.",
                        "Выражение <code>count + total</code> не может иметь тип <code>list[int]</code>, потому что левый"
                                + " операнд — <code>count</code>, а не <code>counts</code>. Кроме того, оператор <code>+</code>"
                                + " соединяет два списка и не добавляет к списку <code>list[int]</code> отдельный элемент"
                                + " типа <code>int</code>."),
                new Offered("variable_confused + operand_type",
                        "Мне показалось, что правый операнд — <code>totals</code>, а не <code>total</code>."
                                + " Результат берёт тип одного из операндов.",
                        "Выражение <code>count + total</code> не может иметь тип <code>list[int]</code>, потому что правый"
                                + " операнд — <code>total</code>, а не <code>totals</code>. Кроме того, оператор <code>+</code>"
                                + " нельзя применить к операндам типов <code>int</code> и <code>list[int]</code>, и ни один"
                                + " из операндов не может стать его результатом.")),
                offered(result));
        assertEquals(Set.of(2L), reasoned(result).reasonings().stream().map(Reasoning::countErrors).collect(Collectors.toSet()));
        assertEquals(Set.of("operand_identification", "sequence_operation_applicability"),
                reasoned(result).reasonings().stream().flatMap(reasoning -> lawNames(reasoning.violations()).stream())
                        .collect(Collectors.toSet()));
    }

    /** Ответ «ошибка» на допустимую операцию объясняют правилом применимости, которому операнды подходят. */
    @Test
    void errorForApplicableOperationExplainsApplicabilityRule() {
        // Arrange.
        var question = question(AVERAGE_OF_GRADES);
        var responses = new ArrayList<AnswerData>(solution(question, AVERAGE_OF_GRADES, 1));
        responses.add(answer(question, "op_div", "t_error"));

        // Act.
        var result = judge(question, responses);

        // Assert.
        assertFalse(result.isAnswerCorrect());
        assertEquals(Set.of(new Hypothesis("inapplicable_assumed", false)), hypotheses(result));
        assertEquals(List.of("numeric_result_type"), lawNames(violations(result)));
        assertEquals(List.of("Выражение <code>total / len(grades)</code> не может вызвать ошибку <code>TypeError</code>, потому что"
                + " оператор <code>/</code> применяется к числам, а оба операнда — числа: <code>int</code> и <code>int</code>."
                + " Следовательно, операция допустима."),
                messages(onlyReasoning(reasoned(result)).explanation()));
    }

    /**
     * Ответ «ошибка», который объясняют и похожая переменная из условия, и мнение, что операция неприменима,
     * получает утверждение, что ошибки не будет, и вопрос, почему студент её ожидал. У причин разные навыки,
     * поэтому до ответа на вопрос ни один из них не засчитывается нарушенным.
     */
    @Test
    void errorExplainedByLookalikeAndApplicabilityAsksWhyErrorWasExpected() {
        // Arrange.
        var question = question(GRADE_COUNT);
        var responses = List.<AnswerData>of(answer(question, "op_len", "t_error"));

        // Act.
        var result = judge(question, responses);

        // Assert.
        assertFalse(result.isAnswerCorrect());
        assertEquals(Map.of("variable_confused", List.of("operand_identification"),
                "inapplicable_assumed", List.of("length_applicability")), violationsByHypothesis(result));
        assertEquals(List.of(), violations(result));
        assertEquals(List.of("Выражение <code>len(grades)</code> не может вызвать ошибку <code>TypeError</code>."),
                messages(reasoned(result).inquiry().statement()));
        assertEquals("Почему вы решили, что здесь возникнет ошибка <code>TypeError</code>?", reasoned(result).inquiry().prompt());
        assertEquals(Set.of(
                new Offered("variable_confused",
                        "Мне показалось, что в выражении стоит <code>grade</code>, а не <code>grades</code>.",
                        "Выражение <code>len(grades)</code> не может вызвать ошибку <code>TypeError</code>, потому что в выражении"
                                + " стоит <code>grades</code>, а не <code>grade</code>."),
                new Offered("inapplicable_assumed",
                        "Функция <code>len</code> не применяется к аргументу этого типа.",
                        "Выражение <code>len(grades)</code> не может вызвать ошибку <code>TypeError</code>, потому что у значения"
                                + " типа <code>list[int]</code> есть элементы, и функция <code>len</code> их считает."
                                + " Следовательно, операция допустима.")),
                offered(result));
    }

    /**
     * Ответ, который объясняют и заблуждение, и прочтение похожей переменной, получает только утверждение, что тип неверен,
     * и уточняющий вопрос с обеими причинами; у каждой причины свой нарушенный навык.
     */
    @Test
    void errorExplainedByMisconceptionAndReadingAsksWhichOne() {
        // Arrange.
        var question = question(GRADES_PLUS_ONE);
        var responses = List.<AnswerData>of(answer(question, "op_add", "t_int"));

        // Act.
        var result = judge(question, responses);

        // Assert.
        assertFalse(result.isAnswerCorrect());
        assertEquals(Map.of("operand_type", List.of("sequence_operation_applicability"),
                "variable_confused", List.of("operand_identification")), violationsByHypothesis(result));
        assertEquals(List.of("Выражение <code>grades + 1</code> не может иметь тип <code>int</code>."),
                messages(reasoned(result).inquiry().statement()));
        assertEquals(Set.of("Результат берёт тип одного из операндов.", "Мне показалось, что левый операнд — <code>grade</code>, а не <code>grades</code>."),
                offered(result).stream().map(Offered::reason).collect(Collectors.toSet()));
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

    /** Если часть выражения вызывает ошибку, подсказка говорит об ошибке, а не о типе. */
    @Test
    void hintForFailingOperationSaysItCausesError() {
        // Arrange.
        var question = withCorrectAnswers(question(FIRST_CHAR_PLUS_ONE), solution(question(FIRST_CHAR_PLUS_ONE), FIRST_CHAR_PLUS_ONE, 1), 1);

        // Act.
        var hint = domain().getAnyNextCorrectAnswer(question, Language.RUSSIAN);

        // Assert.
        var answer = hint.answers.getFirst();
        assertEquals("op_add", answer.left().getDomainInfo());
        assertEquals("t_error", answer.right().getDomainInfo());
        assertEquals(List.of("Выражение <code>line[0] + 1</code> вызывает ошибку <code>TypeError</code>, потому что оператор"
                + " <code>+</code> нельзя применить к операндам типов <code>str</code> и <code>int</code>."), messages(hint.explanation));
    }

    private record Hypothesis(String name, boolean isCorrect) {
    }

    // Рассуждение называется своими гипотезами через « + », от внешнего допущения к вложенному.
    private static String nameOf(Reasoning reasoning) {
        return reasoning.assumptions().stream().map(Assumption::hypothesis).collect(Collectors.joining(" + "));
    }

    // Вердикт без хода мысли гипотез не называет.
    private static Set<Hypothesis> hypotheses(Judgement judgement) {
        return switch (judgement) {
            case Judgement.Verdict verdict -> Set.of();
            case Judgement.Reasoned reasoned -> reasoned.reasonings().stream()
                    .map(reasoning -> new Hypothesis(nameOf(reasoning), reasoning.isCorrect()))
                    .collect(Collectors.toSet());
        };
    }

    private static Reasoning reasoning(Judgement judgement, String hypothesis) {
        var reasonings = reasoned(judgement).reasonings();
        return reasonings.stream()
                .filter(reasoning -> hypothesis.equals(nameOf(reasoning)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Нет рассуждения " + hypothesis + " среди " + reasonings));
    }

    private static Map<String, List<String>> violationsByHypothesis(Judgement judgement) {
        return reasoned(judgement).reasonings().stream()
                .collect(Collectors.toMap(TypeEvaluationDTDomainJudgeTest::nameOf, reasoning -> lawNames(reasoning.violations())));
    }

    // Рассуждение, о котором можно спросить студента, — так, как его увидит уточняющий вопрос.
    private record Offered(String hypothesis, String reason, String explanation) {
    }

    private static Set<Offered> offered(Judgement judgement) {
        return reasoned(judgement).reasonings().stream()
                .filter(reasoning -> reasoning.reason() != null)
                .map(reasoning -> new Offered(nameOf(reasoning), reasoning.reason(),
                        reasoning.explanation().getRawMessage().getText()))
                .collect(Collectors.toSet());
    }

    private static List<String> lawNames(List<ViolationData> violations) {
        return violations.stream().map(ViolationData::getKnowledgeName).distinct().toList();
    }

    private static List<String> messages(Explanation explanation) {
        var parts = explanation.getRawMessage().isEmpty() ? explanation.getChildren() : List.of(explanation);
        return parts.stream().map(part -> part.toHyperText(Language.RUSSIAN).getText()).toList();
    }
}
