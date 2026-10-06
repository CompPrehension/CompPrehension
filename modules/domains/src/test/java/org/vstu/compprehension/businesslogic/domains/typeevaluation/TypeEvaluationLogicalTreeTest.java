package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.nodes.BranchResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.Verdict;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.INAPPLICABLE_ASSUMED;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.OPERAND_TYPE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.RULE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.TYPES;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.judgeSituation;

class TypeEvaluationLogicalTreeTest {

    private static final String RETURNED_OPERAND_SKILL = "logical_returned_operand";
    private static final String TRUTHINESS_MISJUDGED = "truthiness_misjudged";
    private static final String AND_OR_CONFUSED = "and_or_confused";

    /** and и or возвращают операнд, выбранный по истинности левого: or — первый истинный, and — первый ложный. */
    @ParameterizedTest
    @CsvSource({
            "py_or,  t_str,  false, t_list_int, t_list_int",
            "py_or,  t_str,  true,  t_list_int, t_str",
            "py_and, t_int,  false, t_str,      t_int",
            "py_and, t_int,  true,  t_str,      t_str",
            "py_or,  t_bool, true,  t_int,      t_bool",
    })
    void answerWithReturnedOperandTypeIsCorrect(String operation, String leftType, boolean leftTruthy,
                                                String rightType, String answer) {
        // Act.
        var verdict = judgeLogical(operation, leftType, leftTruthy, rightType, answer);

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertTrue(verdict.hypotheses().contains(RULE), verdict.hypotheses().toString());
    }

    /**
     * К верному типу у or с операндами разных типов ведут и две ошибки сразу: неверная истинность левого операнда
     * и перепутанные and/or возвращают тот же операнд, что и правило.
     */
    @Test
    void correctAnswerOfOrIsReachedByTwoCancellingErrors() {
        // Act.
        var verdict = judgeLogical("py_or", "t_str", true, "t_list_int", "t_str");

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertEquals(Set.of(Set.of(RULE), Set.of(TRUTHINESS_MISJUDGED, AND_OR_CONFUSED)), verdict.reasonings());
    }

    /** Если операнды одного типа, ответ объясняет и одна ошибка, поэтому две ошибки сразу в рассуждения не входят. */
    @Test
    void cancellingErrorsAreNotAddedWhenOneErrorExplainsAnswer() {
        // Act.
        var verdict = judgeLogical("py_and", "t_int", true, "t_int", "t_int");

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertEquals(Set.of(Set.of(RULE), Set.of(TRUTHINESS_MISJUDGED), Set.of(AND_OR_CONFUSED)), verdict.reasonings());
    }

    /** Тип правого операнда у or с истинным левым объясняется и неверной истинностью, и перепутанными and/or. */
    @Test
    void otherOperandTypeForTruthyOrHasTwoHypotheses() {
        // Act.
        var verdict = judgeLogical("py_or", "t_str", true, "t_list_int", "t_list_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(TRUTHINESS_MISJUDGED, AND_OR_CONFUSED), verdict.hypotheses());
        assertEquals(Set.of(RETURNED_OPERAND_SKILL), verdict.skills());
    }

    /** Тип левого операнда у and с истинным левым объясняется и неверной истинностью, и перепутанными and/or. */
    @Test
    void otherOperandTypeForTruthyAndHasTwoHypotheses() {
        // Act.
        var verdict = judgeLogical("py_and", "t_int", true, "t_str", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(TRUTHINESS_MISJUDGED, AND_OR_CONFUSED), verdict.hypotheses());
        assertEquals(Set.of(RETURNED_OPERAND_SKILL), verdict.skills());
    }

    /** Ответ bool на or над небулевыми операндами объясняется верой в то, что and/or дают логическое значение. */
    @Test
    void booleanAnswerForOrIsLogicalGivesBoolean() {
        // Act.
        var verdict = judgeLogical("py_or", "t_str", false, "t_list_int", "t_bool");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("logical_gives_boolean"), verdict.hypotheses());
    }

    /** Тип, не совпадающий ни с одним операндом и не bool, — ошибка без гипотез в навыке возвращаемого операнда. */
    @Test
    void unrelatedTypeForOrHasNoHypotheses() {
        // Act.
        var verdict = judgeLogical("py_or", "t_str", false, "t_list_int", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(), verdict.hypotheses());
        assertEquals(Set.of(RETURNED_OPERAND_SKILL), verdict.skills());
    }

    /** Отрицание даёт bool независимо от типа операнда. */
    @Test
    void negationGivesBoolean() {
        // Act.
        var verdict = judgeNegation("t_list_int", "t_bool");

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertEquals(Set.of(RULE), verdict.hypotheses());
    }

    /** Тип операнда в ответе на отрицание объясняется тем, что студент сохранил тип операнда. */
    @Test
    void operandTypeForNegationIsOperandType() {
        // Act.
        var verdict = judgeNegation("t_list_int", "t_list_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(OPERAND_TYPE), verdict.hypotheses());
        assertEquals(Set.of("negation_result"), verdict.skills());
    }

    /** Ответ «ошибка» на and или or объясняется тем, что студент счёл операцию неприменимой к этим операндам. */
    @ParameterizedTest
    @CsvSource({
            "py_or,  true",
            "py_or,  false",
            "py_and, true",
            "py_and, false",
    })
    void errorForReturningLogicalOperationIsInapplicableAssumed(String operation, boolean leftTruthy) {
        // Act.
        var verdict = judgeLogical(operation, "t_str", leftTruthy, "t_list_int", "t_error");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(INAPPLICABLE_ASSUMED), verdict.hypotheses());
        assertEquals(Set.of(RETURNED_OPERAND_SKILL), verdict.skills());
    }

    /** Ответ «ошибка» на отрицание объясняется тем, что студент счёл его неприменимым к операнду. */
    @Test
    void errorForNegationIsInapplicableAssumed() {
        // Act.
        var verdict = judgeNegation("t_list_int", "t_error");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(INAPPLICABLE_ASSUMED), verdict.hypotheses());
        assertEquals(Set.of("negation_result"), verdict.skills());
    }

    private static @NotNull Verdict judgeLogical(@NotNull String operation, @NotNull String leftType, boolean leftTruthy,
                                                 @NotNull String rightType, @NotNull String answerType) {
        return judgeSituation(TYPES + """
                obj a : Variable { hasType(%s); isTruthy = %s; }
                obj b : Variable { hasType(%s); }
                var E = obj op : %s { hasOperand<OperandPlacement:left>(a); hasOperand<OperandPlacement:right>(b); }
                var T = %s
                """.formatted(leftType, leftTruthy, rightType, operation, answerType));
    }

    private static @NotNull Verdict judgeNegation(@NotNull String operandType, @NotNull String answerType) {
        return judgeSituation(TYPES + """
                obj a : Variable { hasType(%s); }
                var E = obj op : py_not { hasOperand<OperandPlacement:left>(a); }
                var T = %s
                """.formatted(operandType, answerType));
    }
}
