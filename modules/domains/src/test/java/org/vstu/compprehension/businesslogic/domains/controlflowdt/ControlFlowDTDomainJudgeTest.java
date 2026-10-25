package org.vstu.compprehension.businesslogic.domains.controlflowdt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.vstu.compprehension.businesslogic.HyperText;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures;
import org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.BankQuestion;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.BackendFactData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.enums.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.interaction;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.answers;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.violation;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.BANK;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.BREAK_IN_FOR;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.IF_ELIF;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.IF_THEN;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.NESTED_CALLS;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.RECURSION_FACTORIAL;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.SEQUENCE;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.WHILE_NOT_ENTERED;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.WHILE_ONE_ITERATION;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.action;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.bankQuestion;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.bankRecord;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.domain;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.trace;

class ControlFlowDTDomainJudgeTest {

    private static final String ACTIONS_IN_ORDER_SKILL = "actions_in_order";
    private static final String REPEAT_SKILL = "passed_action_repeat";
    private static final String CONDITION_VALUE_SKILL = "condition_value_selects_next";
    private static final String INTERRUPTION_SKILL = "interruption_exits_constructs";
    private static final String CALL_RETURN_SKILL = "call_return_matches_activation";
    private static final String BODY_ON_CALL_SKILL = "function_body_runs_on_call";
    private static final String NOT_RESUMED_SKILL = "function_not_resumed_after_exit";
    private static final String SOLUTION_KEY_VERB = "acceptedAnswerIds";
    private static final String TRACE_ACT = "<span class=\"trace-act\" data-action=\"%s\">";
    private static final String CONDITION_OF_FACT = "if_structure_action_first_cond_ast8";
    private static final String THEN_BODY_OF_IF_ELIF = "block_structure_action_first_ast39";
    private static final String BODY_OF_WHILE_NOT_ENTERED = "block_structure_action_first_ast56";
    private static final String BODY_OF_SQUARE = "block_structure_action_first_ast9";
    private static final String INNER_CALL_RETURNS_FACT_2 = "func_call_structure_action_END_ast25,semantic_value_action_25_25_END_1";
    private static final Pattern CYRILLIC = Pattern.compile("[А-Яа-яЁё]");

    static Stream<BankQuestion> bank() {
        return BANK.stream();
    }

    /** Программы, второе действие которых выполняется вне тела функции. */
    static Stream<BankQuestion> bankWithSecondActionOutsideFunctions() {
        return BANK.stream().filter(q -> q != NESTED_CALLS);
    }

    // ---- judgeQuestion ----

    /** Эталонная трасса принимается шаг за шагом до конца. */
    @ParameterizedTest
    @MethodSource("bank")
    void judgeQuestionAcceptsTraceStepByStep(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);
        var given = new ArrayList<AnswerObjectData>();

        for (int step = 0; step < bankQuestion.steps(); step++) {
            given.add(action(question, bankQuestion.action(step)));

            // Act.
            var result = judge(question, given);

            // Assert.
            assertTrue(result.isAnswerCorrect, bankQuestion.file() + " на шаге " + step);
            assertEquals(List.of(), result.violations);
            assertEquals(bankQuestion.steps() - given.size(), result.IterationsLeft);
            assertFalse(result.domainSkills.isEmpty());
        }
    }

    /** Переход по вычисленному условию засчитывает умение выбирать следующее действие по его значению. */
    @Test
    void judgeQuestionCreditsConditionValueAfterCondition() {
        // Arrange.
        var question = bankQuestion(IF_THEN);

        // Act.
        var afterCondition = judge(question, trace(question, IF_THEN, 4));
        var beforeCondition = judge(question, trace(question, IF_THEN, 2));

        // Assert.
        assertTrue(afterCondition.domainSkills.contains(CONDITION_VALUE_SKILL));
        assertFalse(beforeCondition.domainSkills.contains(CONDITION_VALUE_SKILL));
    }

    /** Пропуск первого действия отклоняется как нарушение порядка действий. */
    @ParameterizedTest
    @MethodSource("bankWithSecondActionOutsideFunctions")
    void judgeQuestionRejectsSkippedFirstAction(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);

        // Act.
        var result = judge(question, List.of(action(question, bankQuestion.action(1))));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(bankQuestion.steps(), result.IterationsLeft);
        assertEquals(List.of(ACTIONS_IN_ORDER_SKILL), lawNames(result.violations));
    }

    /** Повтор только что выполненного действия отклоняется. */
    @ParameterizedTest
    @MethodSource("bank")
    void judgeQuestionRejectsRepeatedAction(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);
        var first = action(question, bankQuestion.action(0));

        // Act.
        var result = judge(question, List.of(first, first));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(bankQuestion.steps() - 1, result.IterationsLeft);
        assertEquals(List.of(REPEAT_SKILL), lawNames(result.violations));
    }

    /** Ветка then при ложном условии отклоняется. */
    @Test
    void judgeQuestionRejectsThenBranchWhenConditionIsFalse() {
        // Arrange.
        var question = bankQuestion(IF_ELIF);

        // Act.
        var result = judge(question, withMistake(question, IF_ELIF, 3, THEN_BODY_OF_IF_ELIF));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(IF_ELIF.steps() - 3, result.IterationsLeft);
        assertEquals(List.of(CONDITION_VALUE_SKILL), lawNames(result.violations));
    }

    /** Тело цикла при ложном условии отклоняется. */
    @Test
    void judgeQuestionRejectsLoopBodyWhenConditionIsFalse() {
        // Arrange.
        var question = bankQuestion(WHILE_NOT_ENTERED);

        // Act.
        var result = judge(question, withMistake(question, WHILE_NOT_ENTERED, 4, BODY_OF_WHILE_NOT_ENTERED));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(1, result.IterationsLeft);
        assertEquals(List.of(CONDITION_VALUE_SKILL), lawNames(result.violations));
    }

    /** Новая итерация после ложного условия цикла отклоняется. */
    @Test
    void judgeQuestionRejectsIterationAfterFalseLoopCondition() {
        // Arrange.
        var question = bankQuestion(WHILE_ONE_ITERATION);

        // Act.
        var result = judge(question, withMistake(question, WHILE_ONE_ITERATION, 7, WHILE_ONE_ITERATION.action(3)));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(1, result.IterationsLeft);
        assertEquals(List.of(CONDITION_VALUE_SKILL), lawNames(result.violations));
    }

    /** Продолжение цикла после break отклоняется: прерывание выводит из цикла. */
    @Test
    void judgeQuestionRejectsLoopContinuationAfterBreak() {
        // Arrange.
        var question = bankQuestion(BREAK_IN_FOR);

        // Act.
        var result = judge(question, withMistake(question, BREAK_IN_FOR, 8, BREAK_IN_FOR.action(4)));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(1, result.IterationsLeft);
        assertEquals(List.of(INTERRUPTION_SKILL), lawNames(result.violations));
    }

    /** Выход из цикла по break засчитывает умение выходить из конструкций по прерыванию. */
    @Test
    void judgeQuestionCreditsInterruptionExitAfterBreak() {
        // Arrange.
        var question = bankQuestion(BREAK_IN_FOR);

        // Act.
        var result = judge(question, trace(question, BREAK_IN_FOR));

        // Assert.
        assertTrue(result.isAnswerCorrect);
        assertEquals(0, result.IterationsLeft);
        assertTrue(result.domainSkills.contains(INTERRUPTION_SKILL));
    }

    /** Действие из тела функции до её вызова отклоняется: тело выполняется только при вызове. */
    @Test
    void judgeQuestionRejectsFunctionBodyBeforeCall() {
        // Arrange.
        var question = bankQuestion(NESTED_CALLS);

        // Act.
        var result = judge(question, withMistake(question, NESTED_CALLS, 0, BODY_OF_SQUARE));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(NESTED_CALLS.steps(), result.IterationsLeft);
        assertEquals(List.of(BODY_ON_CALL_SKILL), lawNames(result.violations));
    }

    /** Действие из тела функции после возврата из неё отклоняется: завершённый вызов не продолжается. */
    @Test
    void judgeQuestionRejectsFunctionBodyAfterReturn() {
        // Arrange.
        var question = bankQuestion(NESTED_CALLS);

        // Act.
        var result = judge(question, withMistake(question, NESTED_CALLS, 4, BODY_OF_SQUARE));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(NESTED_CALLS.steps() - 4, result.IterationsLeft);
        assertEquals(List.of(NOT_RESUMED_SKILL), lawNames(result.violations));
    }

    /** Возврат из внешнего рекурсивного вызова, пока не завершился самый внутренний, отклоняется. */
    @Test
    void judgeQuestionRejectsReturnFromOuterRecursiveCall() {
        // Arrange.
        var question = bankQuestion(RECURSION_FACTORIAL);

        // Act.
        var result = judge(question, withMistake(question, RECURSION_FACTORIAL, 8, INNER_CALL_RETURNS_FACT_2));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(RECURSION_FACTORIAL.steps() - 8, result.IterationsLeft);
        assertEquals(List.of(CALL_RETURN_SKILL), lawNames(result.violations));
    }

    /** Вход в тело функции и возврат из вызова с верным значением засчитывают умения вызова функций. */
    @Test
    void judgeQuestionCreditsFunctionCallSkills() {
        // Arrange.
        var question = bankQuestion(RECURSION_FACTORIAL);

        // Act.
        var afterBodyEntry = judge(question, trace(question, RECURSION_FACTORIAL, 3));
        var afterReturn = judge(question, trace(question, RECURSION_FACTORIAL, 9));

        // Assert.
        assertTrue(afterBodyEntry.domainSkills.contains(BODY_ON_CALL_SKILL));
        assertTrue(afterReturn.domainSkills.contains(CALL_RETURN_SKILL));
        assertTrue(afterReturn.domainSkills.contains(NOT_RESUMED_SKILL));
    }

    /** Объяснение ошибки локализовано. */
    @Test
    void judgeQuestionExplainsMistakeInRequestedLanguage() {
        // Arrange.
        var question = bankQuestion(SEQUENCE);
        var wrong = answers(action(question, SEQUENCE.action(1)));

        // Act.
        var english = domain().judgeQuestion(question, wrong, List.of(), Language.ENGLISH).explanation.toHyperText(Language.ENGLISH).getText();
        var russian = domain().judgeQuestion(question, wrong, List.of(), Language.RUSSIAN).explanation.toHyperText(Language.RUSSIAN).getText();

        // Assert.
        assertFalse(english.isBlank());
        assertFalse(CYRILLIC.matcher(english).find(), english);
        assertFalse(english.contains("null"), english);
        assertNotEquals(english, russian);
    }

    /** Значение условия в объяснении ошибки названо на языке студента. */
    @Test
    void judgeQuestionNamesConditionValueInRequestedLanguage() {
        // Arrange.
        var question = bankQuestion(WHILE_NOT_ENTERED);
        var enteredBody = answers(withMistake(question, WHILE_NOT_ENTERED, 4, "block_structure_action_first_ast56"));

        // Act.
        var english = domain().judgeQuestion(question, enteredBody, List.of(), Language.ENGLISH).explanation.toHyperText(Language.ENGLISH).getText();
        var russian = domain().judgeQuestion(question, enteredBody, List.of(), Language.RUSSIAN).explanation.toHyperText(Language.RUSSIAN).getText();

        // Assert.
        assertTrue(russian.contains("ложь"), russian);
        assertFalse(russian.contains("false"), russian);
        assertTrue(english.contains("false"), english);
    }

    /** Вопрос, выданный по старой версии модели, не оценивается. */
    @Test
    void judgeQuestionRejectsUnsupportedQuestionVersion() {
        // Arrange.
        var record = bankRecord(SEQUENCE);
        var question = QuestionData.of(domain().makeQuestion(record, List.of(), Language.RUSSIAN).getContent());
        record.setVersion(2);

        // Act & Assert.
        assertThrows(UnsupportedOperationException.class, () -> judge(question, List.of(action(question, SEQUENCE.action(0)))));
    }

    // ---- сохранённое решение ----

    /** Засчитанный ответ сохраняет решение с ответами, по которым оно построено; незасчитанный решение не меняет. */
    @Test
    void acceptedAnswerUpdatesSolutionAndRejectedDoesNot() {
        // Arrange.
        var question = bankQuestion(IF_THEN);
        var accepted = trace(question, IF_THEN, 2);

        // Act.
        var correct = judge(question, accepted);
        var wrong = judge(question, withMistake(question, IF_THEN, 1, IF_THEN.action(3)));

        // Assert.
        assertNotNull(correct.updatedSolutionFacts);
        assertEquals(answerIds(accepted), solutionKey(correct.updatedSolutionFacts));
        assertNull(wrong.updatedSolutionFacts);
    }

    /** Решение, сохранённое после каждого шага, ведёт к тем же оценкам, что и восстановление трассы по ответам. */
    @ParameterizedTest
    @MethodSource("bank")
    void savedSolutionContinuesTraceLikeReplay(BankQuestion bankQuestion) {
        // Arrange.
        var fresh = bankQuestion(bankQuestion);
        var question = fresh;
        var given = new ArrayList<AnswerObjectData>();

        for (int step = 0; step < bankQuestion.steps(); step++) {
            given.add(action(fresh, bankQuestion.action(step)));

            // Act.
            var saved = judge(question, given);
            var replayed = judge(fresh, given);

            // Assert.
            assertTrue(saved.isAnswerCorrect, bankQuestion.file() + " на шаге " + step);
            assertEquals(replayed.domainSkills, saved.domainSkills, bankQuestion.file() + " на шаге " + step);
            assertEquals(replayed.updatedSolutionFacts.size(), saved.updatedSolutionFacts.size(),
                    bankQuestion.file() + " на шаге " + step);
            question = withSolution(question, saved.updatedSolutionFacts);
        }
    }

    /** Решение, построенное по другим ответам, не используется: трасса восстанавливается по присланным ответам. */
    @Test
    void solutionOfOtherAnswersIsNotUsed() {
        // Arrange.
        var question = bankQuestion(WHILE_ONE_ITERATION);
        var later = judge(question, trace(question, WHILE_ONE_ITERATION, 5)).updatedSolutionFacts;
        var withLaterSolution = withSolution(question, later);

        // Act.
        var result = judge(withLaterSolution, trace(question, WHILE_ONE_ITERATION, 3));

        // Assert.
        assertTrue(result.isAnswerCorrect);
        assertEquals(WHILE_ONE_ITERATION.steps() - 3, result.IterationsLeft);
        assertEquals(answerIds(trace(question, WHILE_ONE_ITERATION, 3)), solutionKey(result.updatedSolutionFacts));
    }

    // ---- getAnyNextCorrectAnswer ----

    /** Подсказки повторяют эталонную трассу. */
    @ParameterizedTest
    @MethodSource("bank")
    void hintsFollowTrace(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);
        var given = new ArrayList<AnswerObjectData>();

        for (int step = 0; step < bankQuestion.steps(); step++) {
            var expected = action(question, bankQuestion.action(step));

            // Act.
            var hint = domain().getAnyNextCorrectAnswer(withCorrectSteps(question, given, bankQuestion), Language.RUSSIAN);

            // Assert.
            assertEquals(1, hint.answers.size(), bankQuestion.file() + " на шаге " + step);
            assertEquals(expected, hint.answers.getFirst().left(), bankQuestion.file() + " на шаге " + step);
            assertEquals(expected, hint.answers.getFirst().right());
            assertNull(hint.lawName);
            given.add(expected);
        }
    }

    /** Ошибочная попытка на подсказку не влияет. */
    @Test
    void hintIgnoresMistakenInteraction() {
        // Arrange.
        var question = bankQuestion(SEQUENCE);
        var mistaken = question.withInteraction(interaction(1L, List.of(action(question, SEQUENCE.action(1))),
                List.of(violation(ACTIONS_IN_ORDER_SKILL)), SEQUENCE.steps()));

        // Act.
        var hint = domain().getAnyNextCorrectAnswer(mistaken, Language.RUSSIAN);

        // Assert.
        assertEquals(SEQUENCE.action(0), hint.answers.getFirst().left().getDomainInfo());
    }

    // ---- getFullSolutionTrace ----

    /** Трасса решения проходит действия всех засчитанных ответов по порядку. */
    @ParameterizedTest
    @MethodSource("bank")
    void solutionTraceListsAnsweredActsInOrder(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);
        var solved = withCorrectSteps(question, trace(question, bankQuestion), bankQuestion);

        // Act.
        var trace = texts(domain().getFullSolutionTrace(solved, Language.RUSSIAN));

        // Assert.
        int line = 0;
        for (String answered : bankQuestion.trace()) {
            String act = TRACE_ACT.formatted(answered.split(",")[0]);
            while (line < trace.size() && !trace.get(line).startsWith(act)) {
                line++;
            }
            assertTrue(line < trace.size(), bankQuestion.file() + ": нет акта " + answered);
            line++;
        }
    }

    /** В трассе только непрозрачные действия: начало программы, входы в ветвление и блок и выходы из них не показываются. */
    @Test
    void solutionTraceShowsOnlyOpaqueActions() {
        // Arrange.
        var question = bankQuestion(IF_THEN);
        var solved = withCorrectSteps(question, trace(question, IF_THEN), IF_THEN);

        // Act.
        var trace = texts(domain().getFullSolutionTrace(solved, Language.RUSSIAN));

        // Assert.
        assertEquals(List.of("global_statements_structure_action_first_ast6", "global_statements_structure_action_next_ast19",
                        "global_statements_structure_action_next_ast66", "if_structure_action_first_cond_ast29",
                        "if_structure_action_if_branch_ast64", "block_structure_action_first_ast43",
                        "global_statements_structure_action_next_ast98", "global_statements_structure_action_next_ast124",
                        "global_statements_structure_action_next_ast131"),
                trace.stream().map(ControlFlowDTDomainJudgeTest::actionOf).toList());
    }

    /** Повторы действия нумеруются, а после тире стоит значение условия. */
    @Test
    void solutionTraceCountsRepeatsAndShowsConditionValues() {
        // Arrange.
        var question = bankQuestion(RECURSION_FACTORIAL);
        var solved = withCorrectSteps(question, trace(question, RECURSION_FACTORIAL), RECURSION_FACTORIAL);

        // Act.
        var conditions = linesOf(texts(domain().getFullSolutionTrace(solved, Language.RUSSIAN)), CONDITION_OF_FACT);

        // Assert.
        assertEquals(3, conditions.size());
        assertTrue(conditions.get(0).contains("1-й") && conditions.get(0).contains(valueHint("ложь")), conditions.get(0));
        assertTrue(conditions.get(1).contains("2-й") && conditions.get(1).contains(valueHint("ложь")), conditions.get(1));
        assertTrue(conditions.get(2).contains("3-й") && conditions.get(2).contains(valueHint("истина")), conditions.get(2));
    }

    /** BEGIN и END вызова называют вызов в его роде и несут подсказку значения, по которой dropdown ищет значения. */
    @Test
    void solutionTraceNamesCallBoundariesWithValueHints() {
        // Arrange.
        var question = bankQuestion(NESTED_CALLS);
        var solved = withCorrectSteps(question, trace(question, NESTED_CALLS), NESTED_CALLS);

        // Act.
        var trace = texts(domain().getFullSolutionTrace(solved, Language.RUSSIAN));

        // Assert.
        var begin = linesOf(trace, "func_call_structure_action_BEGIN_ast25");
        var end = linesOf(trace, "func_call_structure_action_END_ast25");
        assertEquals(1, begin.size());
        assertTrue(begin.getFirst().contains("вызов функции"), begin.getFirst());
        assertTrue(begin.getFirst().contains("начал выполняться"), begin.getFirst());
        assertTrue(begin.getFirst().contains(" — " + valueHint("square(2)")), begin.getFirst());
        assertTrue(end.getFirst().contains("завершил выполнение"), end.getFirst());
        assertTrue(end.getFirst().contains(valueHint("square(2)")), end.getFirst());
    }

    /** Действие без значения выводится без тире. */
    @Test
    void solutionTraceOmitsDashWithoutValue() {
        // Arrange.
        var question = bankQuestion(SEQUENCE);
        var solved = withCorrectSteps(question, trace(question, SEQUENCE), SEQUENCE);

        // Act.
        var trace = texts(domain().getFullSolutionTrace(solved, Language.RUSSIAN));

        // Assert.
        assertTrue(trace.stream().noneMatch(line -> line.contains("—") || line.contains("value-hint")), trace.toString());
    }

    /** До первого ответа трасса пуста. */
    @Test
    void solutionTraceBeforeFirstAnswerIsEmpty() {
        // Act.
        var trace = domain().getFullSolutionTrace(bankQuestion(SEQUENCE), Language.RUSSIAN);

        // Assert.
        assertEquals(List.of(), trace);
    }

    /** Последняя ошибка попадает в трассу подсвеченной, но не как акт трассы. */
    @Test
    void solutionTraceHighlightsLastMistake() {
        // Arrange.
        var question = bankQuestion(SEQUENCE);
        var afterMistake = question.withInteraction(interaction(1L, List.of(action(question, SEQUENCE.action(1))),
                List.of(violation(ACTIONS_IN_ORDER_SKILL)), SEQUENCE.steps()));
        var afterCorrect = question.withInteraction(interaction(2L, List.of(action(question, SEQUENCE.action(0))), List.of(), 1));

        // Act.
        var mistaken = texts(domain().getFullSolutionTrace(afterMistake, Language.RUSSIAN));
        var correct = texts(domain().getFullSolutionTrace(afterCorrect, Language.RUSSIAN));

        // Assert.
        assertEquals(1, mistaken.size());
        assertTrue(mistaken.getLast().startsWith("<span class=\"warning\">"), mistaken.getLast());
        assertFalse(mistaken.getLast().contains("trace-act"), mistaken.getLast());
        assertFalse(correct.getLast().contains("warning"), correct.getLast());
        assertTrue(correct.getLast().startsWith(TRACE_ACT.formatted(SEQUENCE.action(0))), correct.getLast());
    }

    /** Ошибки вперемешку с верными шагами: в трассе верные шаги и только последняя ошибка. */
    @Test
    void solutionTraceKeepsCorrectStepsThroughMistakes() {
        // Arrange.
        var question = bankQuestion(IF_THEN);
        var clicks = List.of(IF_THEN.action(0), IF_THEN.action(1), IF_THEN.action(3), IF_THEN.action(2), IF_THEN.action(4), IF_THEN.action(3));
        var correctSteps = List.of(1, 2, 2, 3, 3, 4);

        for (int step = 0; step < clicks.size(); step++) {
            var given = new ArrayList<>(question.findLatestCorrectAnswers().stream().map(AnswerData::left).toList());
            given.add(action(question, clicks.get(step)));
            var result = judge(question, given);
            question = question.withInteraction(interaction(step + 1, given, result.violations, result.IterationsLeft));
            var reference = texts(domain().getFullSolutionTrace(
                    withCorrectSteps(bankQuestion(IF_THEN), trace(question, IF_THEN, correctSteps.get(step)), IF_THEN), Language.RUSSIAN));

            // Act.
            var trace = texts(domain().getFullSolutionTrace(question, Language.RUSSIAN));

            // Assert.
            assertEquals(correctSteps.get(step) == given.size(), result.isAnswerCorrect, "шаг " + step);
            assertEquals(reference.size() + (result.isAnswerCorrect ? 0 : 1), trace.size(), "шаг " + step);
            assertEquals(reference, trace.subList(0, reference.size()), "шаг " + step);
            if (!result.isAnswerCorrect) {
                assertTrue(trace.getLast().startsWith("<span class=\"warning\">"), trace.getLast());
            }
        }
    }

    /** Трасса локализована, включая значения условий. */
    @Test
    void solutionTraceIsLocalized() {
        // Arrange.
        var question = bankQuestion(IF_THEN);
        var solved = withCorrectSteps(question, trace(question, IF_THEN), IF_THEN);

        // Act.
        var english = texts(domain().getFullSolutionTrace(solved, Language.ENGLISH));
        var russian = texts(domain().getFullSolutionTrace(solved, Language.RUSSIAN));

        // Assert.
        assertEquals(russian.size(), english.size());
        assertTrue(english.getFirst().contains("executed</span> for the"), english.getFirst());
        assertTrue(english.stream().noneMatch(line -> CYRILLIC.matcher(line).find()), english.toString());
        assertTrue(linesOf(english, IF_THEN.action(2)).getFirst().contains(valueHint("true")));
    }

    // ---- вспомогательное ----

    private static Domain.InterpretSentenceResult judge(QuestionData question, List<AnswerObjectData> answers) {
        return domain().judgeQuestion(question, answers(answers), List.of(), Language.RUSSIAN);
    }

    private static List<AnswerObjectData> withMistake(QuestionData question, BankQuestion bankQuestion, int correctSteps, String wrongAction) {
        var given = new ArrayList<>(trace(question, bankQuestion, correctSteps));
        given.add(action(question, wrongAction));
        return given;
    }

    private static List<Integer> answerIds(List<AnswerObjectData> answers) {
        return answers.stream().map(AnswerObjectData::getAnswerId).toList();
    }

    private static List<Integer> solutionKey(List<BackendFactData> solution) {
        return solution.stream()
                .filter(fact -> fact.getVerb().equals(SOLUTION_KEY_VERB))
                .findFirst()
                .map(fact -> Stream.of(fact.getObject().split(",")).map(Integer::valueOf).toList())
                .orElseThrow();
    }

    private static List<String> texts(List<HyperText> trace) {
        return trace.stream().map(HyperText::getText).toList();
    }

    private static List<String> linesOf(List<String> trace, String action) {
        String act = TRACE_ACT.formatted(action);
        return trace.stream().filter(line -> line.startsWith(act)).toList();
    }

    private static String actionOf(String line) {
        return line.substring(line.indexOf("data-action=\"") + "data-action=\"".length(), line.indexOf("\">"));
    }

    private static String valueHint(String value) {
        return "<span class=\"value-hint\">" + value + "</span>";
    }

    private static List<String> lawNames(List<ViolationData> violations) {
        return violations.stream().map(ViolationData::getLawName).toList();
    }

    private static QuestionData withCorrectSteps(QuestionData question, List<AnswerObjectData> given, BankQuestion bankQuestion) {
        return DomainFixtures.withCorrectSteps(question, given, bankQuestion.steps());
    }

    private static QuestionData withSolution(QuestionData question, List<BackendFactData> solutionFacts) {
        return question.withContent(question.getContent().toBuilder().solutionFacts(solutionFacts).build());
    }
}
