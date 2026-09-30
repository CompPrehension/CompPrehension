package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.nodes.BranchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.OPERAND_TYPE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.RULE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.judge;

class TypeEvaluationComparisonTreeTest {

    private static final String ORDERING_SKILL = "ordering_comparison";
    private static final String MEMBERSHIP_SKILL = "membership_test";
    private static final String INAPPLICABLE_GIVES_BOOLEAN = "inapplicable_comparison_gives_boolean";

    /** Применимое сравнение даёт bool, неприменимое — ошибку: равенство — для любых типов, порядок — для чисел и последовательностей одного вида, вхождение — в коллекции и строки. */
    @ParameterizedTest
    @CsvSource({
            "py_eq, t_int,          t_str,          t_bool",
            "py_ne, t_list_int,     t_int,          t_bool",
            "py_lt, t_int,          t_float,        t_bool",
            "py_lt, t_bool,         t_int,          t_bool",
            "py_ge, t_str,          t_str,          t_bool",
            "py_lt, t_list_int,     t_list_int,     t_bool",
            "py_lt, t_str,          t_int,          t_error",
            "py_gt, t_dict_str_int, t_dict_str_int, t_error",
            "py_in, t_int,          t_list_int,     t_bool",
            "py_in, t_str,          t_str,          t_bool",
            "py_in, t_str,          t_dict_str_int, t_bool",
            "py_in, t_int,          t_str,          t_error",
            "py_in, t_int,          t_int,          t_error",
    })
    void answerByComparisonRuleIsCorrect(String operation, String left, String right, String answer) {
        // Act.
        var verdict = judge(operation, left, right, answer);

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertTrue(verdict.hypotheses().contains(RULE), verdict.hypotheses().toString());
    }

    /** Тип операнда в ответе на сравнение чисел объясняется тем, что студент сохранил тип операнда. */
    @Test
    void operandTypeForOrderingIsOperandType() {
        // Act.
        var verdict = judge("py_lt", "t_int", "t_int", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(OPERAND_TYPE), verdict.hypotheses());
        assertEquals(Set.of(ORDERING_SKILL), verdict.skills());
    }

    /** Ответ bool на сравнение строки с числом объясняется тем, что студент считает такие значения сравнимыми. */
    @Test
    void booleanForIncomparableTypesIsInapplicableComparison() {
        // Act.
        var verdict = judge("py_lt", "t_str", "t_int", "t_bool");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(INAPPLICABLE_GIVES_BOOLEAN), verdict.hypotheses());
        assertEquals(Set.of(ORDERING_SKILL), verdict.skills());
    }

    /** Ответ bool на поиск числа в строке объясняется тем, что студент считает такую проверку вхождения допустимой. */
    @Test
    void booleanForNumberInTextIsInapplicableComparison() {
        // Act.
        var verdict = judge("py_in", "t_int", "t_str", "t_bool");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(INAPPLICABLE_GIVES_BOOLEAN), verdict.hypotheses());
        assertEquals(Set.of(MEMBERSHIP_SKILL), verdict.skills());
    }

    /** Тип искомого значения в ответе на проверку вхождения объясняется тем, что студент ждёт найденный элемент. */
    @Test
    void operandTypeForMembershipIsOperandType() {
        // Act.
        var verdict = judge("py_in", "t_int", "t_list_int", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(OPERAND_TYPE), verdict.hypotheses());
        assertEquals(Set.of(MEMBERSHIP_SKILL), verdict.skills());
    }

    /** Тип, не связанный ни с операндами, ни с bool, — ошибка без гипотез в навыке проверки на равенство. */
    @Test
    void unrelatedTypeForEqualityHasNoHypotheses() {
        // Act.
        var verdict = judge("py_eq", "t_int", "t_str", "t_list_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(), verdict.hypotheses());
        assertEquals(Set.of("equality_comparison"), verdict.skills());
    }
}
