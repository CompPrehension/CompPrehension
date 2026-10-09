package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import org.junit.jupiter.api.Test;
import org.vstu.compprehension.businesslogic.HyperText;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.enums.Language;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.AGE_NEXT_YEAR;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.AVERAGE_BY_SUBJECT;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.BankQuestion;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.FIRST_CHAR_PLUS_ONE;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.domain;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.question;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.solution;
import static org.vstu.compprehension.businesslogic.domains.typeevaluation.TypeEvaluationDomainFixture.withCorrectAnswers;

class TypeEvaluationDTDomainSolutionTraceTest {

    /** В трассе у каждой решённой части выражения виден её тип, а если значение известно из условия — и оно. */
    @Test
    void traceShowsValueOfSolvedOperationsAlongWithType() {
        // Arrange.
        var question = solvedQuestion(AVERAGE_BY_SUBJECT, 5);

        // Act.
        var trace = traceOf(question);

        // Assert.
        assertEquals(List.of(
                "<code>totals[subject]</code> вычислено: значение <code>0</code>, тип <code>int</code>",
                "<code>grades[subject]</code> вычислено: значение <code>[]</code>, тип <code>list[int]</code>",
                "<code>len(grades[subject])</code> вычислено: значение <code>0</code>, тип <code>int</code>",
                "<code>len(grades[subject]) or 1</code> вычислено: значение <code>1</code>, тип <code>int</code>",
                "<code>totals[subject] / (len(grades[subject]) or 1)</code> вычислено: значение <code>0.0</code>, тип <code>float</code>"),
                trace);
    }

    /** Если значение зависит от ввода пользователя и не известно студенту, трасса остаётся только с типами. */
    @Test
    void traceHasOnlyTypesWhenValuesAreUnknown() {
        // Arrange.
        var question = solvedQuestion(AGE_NEXT_YEAR, 2);

        // Act.
        var trace = traceOf(question);

        // Assert.
        assertEquals(List.of(
                "<code>name + \", через год вам будет \"</code> вычислено: тип <code>str</code>",
                "<code>int(age)</code> вычислено: тип <code>int</code>"),
                trace);
    }

    /** Часть выражения, которая вызывает ошибку, не имеет значения: в трассе у неё только тип ошибки. */
    @Test
    void traceHasNoValueForOperationThatFails() {
        // Arrange.
        var question = solvedQuestion(FIRST_CHAR_PLUS_ONE, 2);

        // Act.
        var trace = traceOf(question);

        // Assert.
        assertEquals(List.of(
                "<code>line[0]</code> вычислено: значение <code>'5'</code>, тип <code>str</code>",
                "<code>line[0] + 1</code> вычислено: ошибка <code>TypeError</code>"),
                trace);
    }

    private static List<String> traceOf(QuestionData question) {
        return domain().getFullSolutionTrace(question, Language.RUSSIAN).stream().map(HyperText::getText).toList();
    }

    private static QuestionData solvedQuestion(BankQuestion bankQuestion, int steps) {
        var question = question(bankQuestion);
        return withCorrectAnswers(question, solution(question, bankQuestion, steps), 0);
    }
}
