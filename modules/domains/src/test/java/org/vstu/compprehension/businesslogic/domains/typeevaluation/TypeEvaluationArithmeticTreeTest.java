package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.nodes.BranchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.INAPPLICABLE_ASSUMED;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.OPERAND_TYPE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.RULE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.TYPES;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.judge;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.judgeSituation;

class TypeEvaluationArithmeticTreeTest {

    private static final String TRUE_DIVISION_SKILL = "true_division_result";
    private static final String NUMERIC_RESULT_SKILL = "numeric_result_type";
    private static final String SEQUENCE_OPERATION_SKILL = "sequence_operation_applicability";

    /** Ответ по правилу типизации Python засчитывается. */
    @ParameterizedTest
    @CsvSource({
            "py_truediv, t_int,      t_int, t_float",
            "py_add,     t_int,      t_float, t_float",
            "py_floordiv, t_float,   t_int, t_float",
            "py_add,     t_bool,     t_bool, t_int",
            "py_add,     t_str,      t_str, t_str",
            "py_mul,     t_int,      t_list_int, t_list_int",
            "py_add,     t_str,      t_int, t_error",
    })
    void answerByTypingRuleIsCorrect(String operation, String left, String right, String answer) {
        // Act.
        var verdict = judge(operation, left, right, answer);

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertTrue(verdict.hypotheses().contains(RULE), verdict.hypotheses().toString());
    }

    /** Ответ int на деление целых объясняется двумя гипотезами — взят тип операнда и деление как в C — и снижает навык деления. */
    @Test
    void integerAnswerForTrueDivisionHasTwoHypotheses() {
        // Act.
        var verdict = judge("py_truediv", "t_int", "t_int", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(OPERAND_TYPE, "c_style_division"), verdict.hypotheses());
        assertEquals(Set.of(TRUE_DIVISION_SKILL), verdict.skills());
    }

    /** Тип, к которому не ведёт ни одно рассуждение, — ошибка без гипотез, объясняемая правилом деления. */
    @Test
    void unexplainedAnswerHasNoHypotheses() {
        // Act.
        var verdict = judge("py_truediv", "t_int", "t_int", "t_str");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(), verdict.hypotheses());
        assertEquals(Set.of(TRUE_DIVISION_SKILL), verdict.skills());
    }

    /** Ответ int при вещественном операнде объясняется только взятым типом операнда. */
    @Test
    void integerAnswerWithFloatOperandTakesOperandType() {
        // Act.
        var verdict = judge("py_add", "t_int", "t_float", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(OPERAND_TYPE), verdict.hypotheses());
        assertEquals(Set.of(NUMERIC_RESULT_SKILL), verdict.skills());
    }

    /** Ответ int на // с вещественным операндом объясняется и типом операнда, и верой в то, что // даёт целое. */
    @Test
    void integerAnswerForFloorDivisionOfFloatHasTwoHypotheses() {
        // Act.
        var verdict = judge("py_floordiv", "t_float", "t_int", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(OPERAND_TYPE, "floor_division_gives_integer"), verdict.hypotheses());
    }

    /** Ответ bool на сложение логических значений объясняется и типом операнда, и тем, что bool будто остаётся bool. */
    @Test
    void booleanAnswerForBooleanArithmeticHasTwoHypotheses() {
        // Act.
        var verdict = judge("py_add", "t_bool", "t_bool", "t_bool");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(OPERAND_TYPE, "boolean_arithmetic_stays_boolean"), verdict.hypotheses());
        assertEquals(Set.of(NUMERIC_RESULT_SKILL), verdict.skills());
    }

    /** Верный ответ засчитывается, даже если к нему приводит и ошибочное рассуждение. */
    @Test
    void correctAnswerAlsoReachedByMisconceptionIsAccepted() {
        // Act.
        var verdict = judge("py_add", "t_int", "t_int", "t_int");

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertEquals(Set.of(RULE, OPERAND_TYPE), verdict.hypotheses());
    }

    /** Ответ int на сложение строк-чисел объясняется тем, что студент сложил их как числа. */
    @Test
    void integerAnswerForNumericStringsIsNumericTextAddition() {
        // Act.
        var verdict = judgeSituation(TYPES + """
                obj a : Literal { hasType(t_str); isNumericText = true; }
                obj b : Literal { hasType(t_str); isNumericText = true; }
                var E = obj op : py_add { hasOperand<OperandPlacement:left>(a); hasOperand<OperandPlacement:right>(b); }
                var T = t_int
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("numeric_text_addition"), verdict.hypotheses());
        assertEquals(Set.of(SEQUENCE_OPERATION_SKILL), verdict.skills());
    }

    /** Ответ str на сложение строки с числом объясняется и типом операнда, и неявным приведением числа к строке. */
    @Test
    void textAnswerForTextPlusNumberHasTwoHypotheses() {
        // Act.
        var verdict = judge("py_add", "t_str", "t_int", "t_str");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(OPERAND_TYPE, "implicit_text_conversion"), verdict.hypotheses());
    }

    /** Ответ list[int] на сложение списка чисел с числом объясняется и типом операнда, и добавлением элемента в конец списка. */
    @Test
    void listAnswerForListPlusElementHasElementAppended() {
        // Act.
        var verdict = judge("py_add", "t_list_int", "t_int", "t_list_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(OPERAND_TYPE, "element_appended"), verdict.hypotheses());
        assertEquals(Set.of(SEQUENCE_OPERATION_SKILL), verdict.skills());
    }

    /** Список, сложенный с числом другого типа, не объясняют добавлением элемента: элемент такого списка — не это число. */
    @Test
    void listPlusForeignNumberIsNotElementAppended() {
        // Act.
        var verdict = judge("py_add", "t_list_int", "t_float", "t_list_int");

        // Assert.
        assertEquals(Set.of(OPERAND_TYPE), verdict.hypotheses());
    }

    /**
     * Приняв «/» за деление нацело, студент дальше считает тип результата как у остальных операций и может ошибиться
     * и там: ответ int на float / int объясняет и тип операнда, и такое сочетание двух ошибок.
     */
    @Test
    void integerDivisionBeliefContinuesIntoResultType() {
        // Act.
        var verdict = judge("py_truediv", "t_float", "t_int", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of(OPERAND_TYPE), Set.of("c_style_division", OPERAND_TYPE)), verdict.reasonings());
    }

    /** Ответ «ошибка» на допустимую арифметику объясняется тем, что студент счёл операцию неприменимой к этим операндам. */
    @ParameterizedTest
    @CsvSource({
            "py_truediv, t_int, t_int,   numeric_result_type",
            "py_add,     t_int, t_float, numeric_result_type",
            "py_add,     t_int, t_int,   numeric_result_type",
            "py_add,     t_str, t_str,   sequence_operation_applicability",
            "py_mul,     t_list_int, t_int, sequence_operation_applicability",
    })
    void errorForApplicableArithmeticIsInapplicableAssumed(String operation, String leftType, String rightType, String skill) {
        // Act.
        var verdict = judge(operation, leftType, rightType, "t_error");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(INAPPLICABLE_ASSUMED), verdict.hypotheses());
        assertEquals(Set.of(skill), verdict.skills());
    }

}
