package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.nodes.BranchResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.Verdict;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.RULE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.TYPES;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.judgeSituation;

class TypeEvaluationCallTreeTest {

    private static final String ARGUMENT_TYPE = "argument_type";
    private static final String INAPPLICABLE_GIVES_RESULT = "inapplicable_call_gives_result";
    private static final String LENGTH_SKILL = "length_applicability";
    private static final String CONVERSION_SKILL = "conversion_applicability";

    /** Функция возвращает свой тип, если аргумент ей подходит: длина — у коллекций и строк, в число — числа и строки, в строку — что угодно. */
    @ParameterizedTest
    @CsvSource({
            "py_len,        t_str,          t_int",
            "py_len,        t_list_int,     t_int",
            "py_len,        t_dict_str_int, t_int",
            "py_len,        t_int,          t_error",
            "py_int_call,   t_str,          t_int",
            "py_int_call,   t_float,        t_int",
            "py_float_call, t_int,          t_float",
            "py_int_call,   t_list_int,     t_error",
            "py_str_call,   t_int,          t_str",
            "py_str_call,   t_list_int,     t_str",
    })
    void answerByCallRuleIsCorrect(String function, String argument, String answer) {
        // Act.
        var verdict = judgeCall(function, argument, answer);

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertTrue(verdict.hypotheses().contains(RULE), verdict.hypotheses().toString());
    }

    /** Функция ввода возвращает строку. */
    @Test
    void inputGivesText() {
        // Act.
        var verdict = judgeInput("t_str");

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertEquals(Set.of(RULE), verdict.hypotheses());
    }

    /** Числовой ответ на вызов функции ввода объясняется тем, что студент ждёт от неё число. */
    @ParameterizedTest
    @ValueSource(strings = {"t_int", "t_float"})
    void numericAnswerForInputIsInputReturnsNumber(String answer) {
        // Act.
        var verdict = judgeInput(answer);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("input_returns_number"), verdict.hypotheses());
        assertEquals(Set.of("call_fixed_result"), verdict.skills());
    }

    /** Тип коллекции в ответе на длину объясняется тем, что студент сохранил тип аргумента. */
    @Test
    void argumentTypeForLengthIsArgumentType() {
        // Act.
        var verdict = judgeCall("py_len", "t_list_int", "t_list_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(ARGUMENT_TYPE), verdict.hypotheses());
        assertEquals(Set.of(LENGTH_SKILL), verdict.skills());
    }

    /** Ответ int на длину числа объясняется и верой в то, что длина есть у всего, и типом аргумента. */
    @Test
    void integerForLengthOfNumberHasTwoHypotheses() {
        // Act.
        var verdict = judgeCall("py_len", "t_int", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(INAPPLICABLE_GIVES_RESULT, ARGUMENT_TYPE), verdict.hypotheses());
        assertEquals(Set.of(LENGTH_SKILL), verdict.skills());
    }

    /** Ответ float на преобразование float в int объясняется тем, что студент считает, что преобразование сохраняет тип. */
    @Test
    void argumentTypeForConversionIsArgumentType() {
        // Act.
        var verdict = judgeCall("py_int_call", "t_float", "t_float");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(ARGUMENT_TYPE), verdict.hypotheses());
        assertEquals(Set.of(CONVERSION_SKILL), verdict.skills());
    }

    /** Ответ int на преобразование списка в число объясняется тем, что студент считает преобразование возможным. */
    @Test
    void integerForConversionOfListIsInapplicableCall() {
        // Act.
        var verdict = judgeCall("py_int_call", "t_list_int", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(INAPPLICABLE_GIVES_RESULT), verdict.hypotheses());
        assertEquals(Set.of(CONVERSION_SKILL), verdict.skills());
    }

    /** Тип аргумента в ответе на преобразование в строку объясняется тем, что студент сохранил тип аргумента. */
    @Test
    void argumentTypeForTextConversionIsArgumentType() {
        // Act.
        var verdict = judgeCall("py_str_call", "t_int", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(ARGUMENT_TYPE), verdict.hypotheses());
        assertEquals(Set.of("call_fixed_result"), verdict.skills());
    }

    private static @NotNull Verdict judgeCall(@NotNull String function, @NotNull String argumentType,
                                              @NotNull String answerType) {
        return judgeSituation(TYPES + """
                obj x : Variable { hasType(%s); }
                var E = obj op : %s { hasOperand<OperandPlacement:left>(x); }
                var T = %s
                """.formatted(argumentType, function, answerType));
    }

    private static @NotNull Verdict judgeInput(@NotNull String answerType) {
        return judgeSituation(TYPES + """
                var E = obj op : py_input {}
                var T = %s
                """.formatted(answerType));
    }
}
