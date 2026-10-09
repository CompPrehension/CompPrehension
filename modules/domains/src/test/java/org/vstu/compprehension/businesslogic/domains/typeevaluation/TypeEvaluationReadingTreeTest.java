package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.nodes.BranchResult;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.INAPPLICABLE_ASSUMED;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.OPERAND_TYPE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.RULE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.TYPES;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.judgeSituation;

class TypeEvaluationReadingTreeTest {

    private static final String NAME_CONFUSED = "name_confused";
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

    // class Student: name: str; grade: int; grades: list[int]; def average(self) -> float; def averages(self) -> list[int];
    // def names(self) -> list[int]; student = Student(...)
    private static final String STUDENT = TYPES + """
            obj name : Field { hasType(t_str); isStatic = false; visibility = Visibility:public; looksLike(names); }
            obj grade : Field { hasType(t_int); isStatic = false; visibility = Visibility:public; looksLike(grades); }
            obj grades : Field { hasType(t_list_int); isStatic = false; visibility = Visibility:public; }
            obj average : Method { returns(t_float); isStatic = false; visibility = Visibility:public; mutatesReceiver = false; looksLike(averages); }
            obj averages : Method { returns(t_list_int); isStatic = false; visibility = Visibility:public; mutatesReceiver = false; }
            obj names : Method { returns(t_list_int); isStatic = false; visibility = Visibility:public; mutatesReceiver = false; }
            obj t_student : py_class { declares(name); declares(grade); declares(grades); declares(average); declares(averages); declares(names); }
            obj Student : ClassReference { refersTo(t_student); looksLike(student); }
            obj student : Variable { hasType(t_student); }
            obj one : Literal { hasType(t_int); intValue = 1; }
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
        assertEquals(Set.of(Set.of(NAME_CONFUSED), Set.of(INAPPLICABLE_ASSUMED)), verdict.reasonings());
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
        assertTrue(verdict.hypotheses().contains(NAME_CONFUSED), verdict.hypotheses().toString());
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
        assertEquals(Set.of(Set.of(OPERAND_TYPE), Set.of(NAME_CONFUSED)), verdict.reasonings());
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
        assertEquals(Set.of(Set.of(NAME_CONFUSED), Set.of(INAPPLICABLE_ASSUMED)), verdict.reasonings());
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
        assertEquals(Set.of(Set.of(NAME_CONFUSED, OPERAND_TYPE), Set.of(NAME_CONFUSED, "element_appended")),
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
        assertTrue(verdict.reasonings().contains(Set.of(NAME_CONFUSED, OPERAND_TYPE)), verdict.reasonings().toString());
    }

    /**
     * Верный ответ остаётся верным, даже если в условии есть похожая переменная: путаница ведёт к нему только вместе
     * с заблуждением, и такие ошибочные рассуждения сохраняются.
     */
    @Test
    void correctAnswerWithLookalikeVariableIsAccepted() {
        // Act.
        var verdict = judgeSituation(GRADES + """
                var E = obj op : py_len { hasOperand<OperandPlacement:left>(grades); }
                var T = t_int
                """);

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertEquals(Set.of(Set.of(RULE), Set.of(NAME_CONFUSED, "argument_type"),
                Set.of(NAME_CONFUSED, "inapplicable_call_gives_result")), verdict.reasonings());
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

    /**
     * Тип поля в ответ на Student.name объясняют и вера, что поле объекта читается через класс, и прочтение класса
     * Student как похожей переменной student.
     */
    @Test
    void fieldTypeForInstanceFieldViaClassIsClassReadAsVariable() {
        // Act.
        var verdict = judgeSituation(STUDENT + """
                var E = obj op : py_field_access { hasOperand<OperandPlacement:left>(Student); accesses(name); }
                var T = t_str
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of("instance_member_via_class"), Set.of(NAME_CONFUSED)), verdict.reasonings());
    }

    /**
     * Ответ TypeError на student.average() объясняет прочтение объекта student как похожего класса Student, а также
     * мнение, что метода нет, с путаницей видов ошибок — и само по себе, и вместе с тем же прочтением.
     */
    @Test
    void errorForMethodOfObjectIsVariableReadAsClass() {
        // Act.
        var verdict = judgeSituation(STUDENT + """
                var E = obj op : py_method_call { hasOperand<OperandPlacement:left>(student); accesses(average); }
                var T = t_error
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of(NAME_CONFUSED), Set.of("missing_member_assumed", "error_kind_confused"),
                Set.of(NAME_CONFUSED, "missing_member_assumed", "error_kind_confused")), verdict.reasonings());
    }

    /** Вне обращения к члену переменная не читается как похожий на неё класс: у имени класса нет типа. */
    @Test
    void variableIsNotReadAsClassOutsideMemberAccess() {
        // Act.
        var verdict = judgeSituation(STUDENT + """
                var E = obj op : py_add { hasOperand<OperandPlacement:left>(student); hasOperand<OperandPlacement:right>(one); }
                var T = t_error
                """);

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertEquals(Set.of(Set.of(RULE)), verdict.reasonings());
    }

    /** Ответ list[int] на student.grade объясняет прочтение поля grade как похожего поля grades. */
    @Test
    void fieldTypeOfLookalikeFieldIsNameConfused() {
        // Act.
        var verdict = judgeSituation(STUDENT + """
                var E = obj op : py_field_access { hasOperand<OperandPlacement:left>(student); accesses(grade); }
                var T = t_list_int
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of(NAME_CONFUSED)), verdict.reasonings());
    }

    /** Ответ list[int] на student.average() объясняет прочтение метода average как похожего метода averages. */
    @Test
    void resultOfLookalikeMethodIsNameConfused() {
        // Act.
        var verdict = judgeSituation(STUDENT + """
                var E = obj op : py_method_call { hasOperand<OperandPlacement:left>(student); accesses(average); }
                var T = t_list_int
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(Set.of(NAME_CONFUSED)), verdict.reasonings());
    }

    /**
     * Прочтение поля как похожего поля другого класса не объясняет произвольный ответ: решив вдобавок, что такое поле
     * есть у объекта, студент рассуждал бы уже о выдуманном.
     */
    @Test
    void foreignLookalikeFieldDoesNotExplainAnyAnswer() {
        // Act.
        var verdict = judgeSituation(TYPES + """
                obj grade : Field { hasType(t_int); isStatic = false; visibility = Visibility:public; looksLike(grades); }
                obj grades : Field { hasType(t_list_int); isStatic = false; visibility = Visibility:public; }
                obj t_student : py_class { declares(grade); }
                obj t_group : py_class { declares(grades); }
                obj student : Variable { hasType(t_student); }
                var E = obj op : py_field_access { hasOperand<OperandPlacement:left>(student); accesses(grade); }
                var T = t_float
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(), verdict.reasonings());
    }

    /** Поле не читается как похожий на него метод: путаются только имена одного вида. */
    @Test
    void fieldIsNotReadAsLookalikeMethod() {
        // Act.
        var verdict = judgeSituation(STUDENT + """
                var E = obj op : py_field_access { hasOperand<OperandPlacement:left>(student); accesses(name); }
                var T = t_list_int
                """);

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(), verdict.reasonings());
    }
}
