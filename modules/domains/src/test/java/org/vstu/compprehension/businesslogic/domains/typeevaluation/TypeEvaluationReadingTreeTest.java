package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.nodes.BranchResult;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.INAPPLICABLE_ASSUMED;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.OPERAND_TYPE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.RULE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.TYPES;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.judgeSituation;

class TypeEvaluationReadingTreeTest {

    private static final String VARIABLE_CONFUSED = "variable_confused";
    private static final String OPERAND_IDENTIFICATION = "operand_identification";

    // grade = 5; grades = [5, 4, 5]
    private static final String GRADES = TYPES + """
            obj grade : Variable { hasType(t_int); looksLike(grades); }
            obj grades : Variable { hasType(t_list_int); }
            obj one : Literal { hasType(t_int); intValue = 1; }
            """;

    // count = 3; total = 14; counts = [1, 2]; totals = [5, 9]
    private static final String COUNTS = TYPES + """
            obj count : Variable { hasType(t_int); }
            obj counts : Variable { hasType(t_list_int); looksLike(count); }
            obj total : Variable { hasType(t_int); }
            obj totals : Variable { hasType(t_list_int); looksLike(total); }
            """;

    /**
     * Ответ «ошибка» на len(grades), верный для len(grade), объясняют и прочтение grade вместо grades с верным
     * правилом, и мнение, что len неприменима к списку.
     */
    @Test
    void errorForLengthOfLookalikeVariableIsVariableConfused() {
        // Act.
        var verdict = judgeSituation(GRADES + """
                var E = obj op : py_len { hasOperand<OperandPlacement:left>(grades); }
                var T = t_error
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of(VARIABLE_CONFUSED), Set.of(INAPPLICABLE_ASSUMED)), verdict.reasonings());
        assertEquals(Set.of(OPERAND_IDENTIFICATION, "length_applicability"), verdict.skills());
    }

    /** Похожую переменную дерево находит в обе стороны: связь сходства записана один раз. */
    @Test
    void lookalikeIsFoundFromEitherName() {
        // Act.
        var verdict = judgeSituation(GRADES + """
                var E = obj op : py_add { hasOperand<OperandPlacement:left>(grade); hasOperand<OperandPlacement:right>(one); }
                var T = t_error
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertTrue(verdict.hypotheses().contains(VARIABLE_CONFUSED), verdict.hypotheses().toString());
    }

    /** Ответ int на grades + 1 объясняют и тип операнда, и прочитанная вместо grades переменная grade с верным правилом. */
    @Test
    void answerExplainedByMisconceptionAndReadingHasBothHypotheses() {
        // Act.
        var verdict = judgeSituation(GRADES + """
                var E = obj op : py_add { hasOperand<OperandPlacement:left>(grades); hasOperand<OperandPlacement:right>(one); }
                var T = t_int
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of(OPERAND_TYPE), Set.of(VARIABLE_CONFUSED)), verdict.reasonings());
    }

    /** Ответ «ошибка» на total / 2, верный для totals / 2, объясняет в том числе прочитанная вместо total переменная totals. */
    @Test
    void errorForDivisionOfLookalikeVariableIsVariableConfused() {
        // Act.
        var verdict = judgeSituation(TYPES + """
                obj total : Variable { hasType(t_int); looksLike(totals); }
                obj totals : Variable { hasType(t_list_int); }
                obj two : Literal { hasType(t_int); intValue = 2; }
                var E = obj op : py_truediv { hasOperand<OperandPlacement:left>(total); hasOperand<OperandPlacement:right>(two); }
                var T = t_error
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of(VARIABLE_CONFUSED), Set.of(INAPPLICABLE_ASSUMED)), verdict.reasonings());
    }

    /**
     * Ответ list[int] на count + total объясняют только сочетания ошибок: операнд прочитан как похожий список,
     * а результат взят по типу операнда или число добавлено в конец списка.
     */
    @Test
    void answerExplainedOnlyByConfusionWithMisconceptionCombinesThem() {
        // Act.
        var verdict = judgeSituation(COUNTS + """
                var E = obj op : py_add { hasOperand<OperandPlacement:left>(count); hasOperand<OperandPlacement:right>(total); }
                var T = t_list_int
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of(VARIABLE_CONFUSED, OPERAND_TYPE), Set.of(VARIABLE_CONFUSED, "element_appended")),
                verdict.reasonings());
    }

    /** Верный ответ int на count + total, к которому ведёт и путаница с заблуждением, остаётся верным. */
    @Test
    void correctAnswerReachedByCombinedErrorsKeepsMisreasoning() {
        // Act.
        var verdict = judgeSituation(COUNTS + """
                var E = obj op : py_add { hasOperand<OperandPlacement:left>(count); hasOperand<OperandPlacement:right>(total); }
                var T = t_int
                """);

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertTrue(verdict.reasonings().contains(Set.of(VARIABLE_CONFUSED, OPERAND_TYPE)), verdict.reasonings().toString());
    }

    /** Верный ответ остаётся верным, даже если в условии есть похожая переменная. */
    @Test
    void correctAnswerWithLookalikeVariableIsAccepted() {
        // Act.
        var verdict = judgeSituation(GRADES + """
                var E = obj op : py_len { hasOperand<OperandPlacement:left>(grades); }
                var T = t_int
                """);

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertTrue(verdict.hypotheses().contains(RULE), verdict.hypotheses().toString());
        assertFalse(verdict.hypotheses().contains(VARIABLE_CONFUSED), verdict.hypotheses().toString());
    }

    /** Без похожих имён ответ «ошибка» не списывается на путаницу переменных. */
    @Test
    void errorWithoutLookalikeVariableIsNotVariableConfused() {
        // Act.
        var verdict = judgeSituation(TYPES + """
                obj grade : Variable { hasType(t_int); }
                obj grades : Variable { hasType(t_list_int); }
                var E = obj op : py_len { hasOperand<OperandPlacement:left>(grades); }
                var T = t_error
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(INAPPLICABLE_ASSUMED), verdict.hypotheses());
    }
}
