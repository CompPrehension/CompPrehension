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
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.foreignVariant;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.judge;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.judgeSituation;

class TypeEvaluationArithmeticTreeTest {

    private static final String TRUE_DIVISION_SKILL = "true_division_result";
    private static final String NUMERIC_RESULT_SKILL = "numeric_result_type";
    private static final String SEQUENCE_OPERATION_SKILL = "sequence_operation_applicability";
    private static final String LANGUAGE_TRANSFER_SKILL = "language_semantics_transfer";

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

    /**
     * Ответ int на деление целых объясняется двумя гипотезами: взят тип операнда или «/» прочитан как деление, которое
     * над целыми даёт целое, как в некоторых других языках. Навыки у них разные: деление и перенос правил языка.
     */
    @Test
    void integerAnswerForTrueDivisionHasTwoHypotheses() {
        // Act.
        var verdict = judge("py_truediv", "t_int", "t_int", "t_int", foreignVariant("py_integer_division"));

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(OPERAND_TYPE, "foreign_semantics"), verdict.hypotheses());
        assertEquals(Set.of(TRUE_DIVISION_SKILL, LANGUAGE_TRANSFER_SKILL), verdict.skills());
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

    /** Ответ int на сложение строк из цифр объясняется сложением как в других языках, где «+» складывает их как числа. */
    @Test
    void integerAnswerForNumericStringsIsAdditionOfOtherLanguages() {
        // Act.
        var verdict = judgeSituation(TYPES + """
                obj a : Literal { hasType(t_str); isNumericText = true; }
                obj b : Literal { hasType(t_str); isNumericText = true; }
                var E = obj op : py_add { hasOperand<OperandPlacement:left>(a); hasOperand<OperandPlacement:right>(b); }
                var T = t_int
                """ + foreignVariant("py_add_numeric_text"));

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("foreign_semantics"), verdict.hypotheses());
        assertEquals(Set.of(LANGUAGE_TRANSFER_SKILL), verdict.skills());
    }

    /** Ответ str на сложение строки с числом объясняется и типом операнда, и сложением, которое само превращает число в строку. */
    @Test
    void textAnswerForTextPlusNumberHasTwoHypotheses() {
        // Act.
        var verdict = judge("py_add", "t_str", "t_int", "t_str", foreignVariant("py_add_text_conversion"));

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(OPERAND_TYPE, "foreign_semantics"), verdict.hypotheses());
    }

    /**
     * Ответ list[int] на сложение списка чисел с числом объясняется типом операнда, добавлением элемента в конец списка
     * и поэлементным сложением, как в некоторых других языках.
     */
    @Test
    void listAnswerForListPlusElementHasThreeHypotheses() {
        // Act.
        var verdict = judge("py_add", "t_list_int", "t_int", "t_list_int", foreignVariant("py_add_elementwise"));

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(OPERAND_TYPE, "element_appended", "foreign_semantics"), verdict.hypotheses());
        assertEquals(Set.of(SEQUENCE_OPERATION_SKILL, LANGUAGE_TRANSFER_SKILL), verdict.skills());
    }

    /**
     * Список, сложенный с числом другого типа, не объясняют добавлением элемента: элемент такого списка — не это число.
     * Поэлементное сложение и тип операнда его объясняют.
     */
    @Test
    void listPlusForeignNumberIsNotElementAppended() {
        // Act.
        var verdict = judge("py_add", "t_list_int", "t_float", "t_list_int", foreignVariant("py_add_elementwise"));

        // Assert.
        assertEquals(Set.of(OPERAND_TYPE, "foreign_semantics"), verdict.hypotheses());
    }

    /**
     * Прочтение операции по правилам другого языка не предлагается, если оно не меняет вердикт: над float «/» и в
     * других языках даёт float, поэтому ответ int на float / int объясняет только взятый тип операнда.
     */
    @Test
    void foreignReadingThatKeepsVerdictIsNotOffered() {
        // Act.
        var verdict = judge("py_truediv", "t_float", "t_int", "t_int", foreignVariant("py_integer_division"));

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of(OPERAND_TYPE)), verdict.reasonings());
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

    /** Ответ AttributeError на str + int объясняется путаницей видов ошибок: операция неприменима, но ошибка другая. */
    @Test
    void errorOfOtherKindForInapplicableArithmeticIsErrorKindConfused() {
        // Act.
        var verdict = judge("py_add", "t_str", "t_int", "t_attribute_error");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("error_kind_confused"), verdict.hypotheses());
        assertEquals(Set.of("error_kind"), verdict.skills());
    }

}
