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
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.RULE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.TYPES;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationTreeFixture.judgeSituation;

class TypeEvaluationIndexingTreeTest {

    private static final String ELEMENT_SKILL = "indexed_element_type";
    private static final String CONTAINER_TYPE = "container_type";
    private static final String INDEX_TYPE = "index_type";

    /** Индекс даёт элемент последовательности или значение словаря; нецелый индекс последовательности и индекс у числа — ошибка. */
    @ParameterizedTest
    @CsvSource({
            "t_list_int,      t_int, t_int",
            "t_str,           t_int, t_str",
            "t_list_list_int, t_int, t_list_int",
            "t_dict_str_int,  t_str, t_int",
            "t_list_int,      t_str, t_error",
            "t_int,           t_int, t_error",
    })
    void answerByIndexingRuleIsCorrect(String container, String index, String answer) {
        // Act.
        var verdict = judgeIndexing(container, index, answer);

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertTrue(verdict.hypotheses().contains(RULE), verdict.hypotheses().toString());
    }

    /** Элемент кортежа имеет тип своей позиции, позиции считаются с нуля. */
    @ParameterizedTest
    @CsvSource({
            "0, t_str",
            "1, t_int",
    })
    void tupleItemHasTypeOfItsPosition(int position, String answer) {
        // Act.
        var verdict = judgeTupleIndexing(position, answer);

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertTrue(verdict.hypotheses().contains(RULE), verdict.hypotheses().toString());
    }

    /** Тип списка в ответе на обращение по индексу объясняется тем, что студент путает индекс со срезом. */
    @Test
    void containerTypeForListIndexingIsContainerType() {
        // Act.
        var verdict = judgeIndexing("t_list_int", "t_int", "t_list_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of(CONTAINER_TYPE), verdict.hypotheses());
        assertEquals(Set.of(ELEMENT_SKILL), verdict.skills());
    }

    /** Ответ int на обращение к строке матрицы объясняется и пропущенным уровнем вложенности, и типом индекса. */
    @Test
    void innerElementTypeForNestedListHasTwoHypotheses() {
        // Act.
        var verdict = judgeIndexing("t_list_list_int", "t_int", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("nesting_level_skipped", INDEX_TYPE), verdict.hypotheses());
        assertEquals(Set.of(ELEMENT_SKILL), verdict.skills());
    }

    /** Тип ключа в ответе на обращение к словарю объясняется тем, что студент ждёт ключ вместо значения. */
    @Test
    void keyTypeForDictionaryAccessIsKeyInsteadOfValue() {
        // Act.
        var verdict = judgeIndexing("t_dict_str_int", "t_str", "t_str");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("key_instead_of_value"), verdict.hypotheses());
        assertEquals(Set.of("mapping_value_access"), verdict.skills());
    }

    /** Тип элемента в ответе на обращение к списку по строке объясняется тем, что студент не проверил тип индекса. */
    @Test
    void elementTypeForTextIndexIsIndexTypeIgnored() {
        // Act.
        var verdict = judgeIndexing("t_list_int", "t_str", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("index_type_ignored"), verdict.hypotheses());
        assertEquals(Set.of("indexing_applicability"), verdict.skills());
    }

    /** Срез даёт последовательность того же типа; срез словаря и числа — ошибка. */
    @ParameterizedTest
    @CsvSource({
            "t_list_int,     t_list_int",
            "t_str,          t_str",
            "t_dict_str_int, t_error",
            "t_int,          t_error",
    })
    void answerBySlicingRuleIsCorrect(String container, String answer) {
        // Act.
        var verdict = judgeSlicing(container, answer);

        // Assert.
        assertEquals(BranchResult.CORRECT, verdict.result());
        assertTrue(verdict.hypotheses().contains(RULE), verdict.hypotheses().toString());
    }

    /** Тип элемента в ответе на срез списка объясняется тем, что студент путает срез с индексом. */
    @Test
    void elementTypeForListSliceIsSliceAsIndex() {
        // Act.
        var verdict = judgeSlicing("t_list_int", "t_int");

        // Assert.
        assertEquals(BranchResult.ERROR, verdict.result());
        assertEquals(Set.of("slice_as_index"), verdict.hypotheses());
        assertEquals(Set.of("slice_result"), verdict.skills());
    }

    /** Срез кортежа не оценивается: его тип зависит от выбранных позиций, а такого вывода в модели нет. */
    @Test
    void tupleSliceIsNotJudged() {
        // Act.
        var verdict = judgeSlicing("t_tuple_str_int", "t_tuple_str_int");

        // Assert.
        assertEquals(BranchResult.NULL, verdict.result());
    }

    private static @NotNull Verdict judgeIndexing(@NotNull String containerType, @NotNull String indexType,
                                                  @NotNull String answerType) {
        return judgeSituation(TYPES + """
                obj x : Variable { hasType(%s); }
                obj i : Variable { hasType(%s); }
                var E = obj op : py_index { hasOperand<OperandPlacement:left>(x); hasOperand<OperandPlacement:right>(i); }
                var T = %s
                """.formatted(containerType, indexType, answerType));
    }

    private static @NotNull Verdict judgeTupleIndexing(int position, @NotNull String answerType) {
        return judgeSituation(TYPES + """
                obj x : Variable { hasType(t_tuple_str_int); }
                obj i : Literal { hasType(t_int); intValue = %d; }
                var E = obj op : py_index { hasOperand<OperandPlacement:left>(x); hasOperand<OperandPlacement:right>(i); }
                var T = %s
                """.formatted(position, answerType));
    }

    private static @NotNull Verdict judgeSlicing(@NotNull String containerType, @NotNull String answerType) {
        return judgeSituation(TYPES + """
                obj x : Variable { hasType(%s); }
                var E = obj op : py_slice { hasOperand<OperandPlacement:left>(x); }
                var T = %s
                """.formatted(containerType, answerType));
    }
}
