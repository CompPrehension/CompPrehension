package org.vstu.compprehension.businesslogic.domains.controlflowdt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures;
import org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.BankQuestion;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.enums.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.interaction;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.responses;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.violation;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.BANK;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.BREAK_IN_FOR;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.IF_ELIF;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.SEQUENCE;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.WHILE_NOT_ENTERED;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.WHILE_ONE_ITERATION;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.action;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.bankQuestion;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.bankRecord;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.domain;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.trace;

class ControlFlowDTDomainJudgeTest {

    private static final String SEQUENTIAL_ORDER_SKILL = "unevaluated_conditions_between_points_present";
    private static final String REPEATED_ACTION_SKILL = "current_execution_point_understood";
    private static final String FAR_AWAY_ACTION_SKILL = "current_code_block_identified";
    private static final String CONDITION_TRANSITION_SKILL = "applicable_transition_with_condition_found";
    private static final String INTERRUPTION_TRANSITION_SKILL = "applicable_transition_with_interruption_found";
    private static final String INTERRUPTION_TERMINATED_SKILL = "interruption_can_be_terminated_determined";
    private static final String INTERRUPTION_MODE_SKILL = "transition_matches_interruption_mode";
    private static final String CONDITION_VALUE_SKILL = "required_condition_value_determined";
    private static final Pattern CYRILLIC = Pattern.compile("[А-Яа-яЁё]");

    static Stream<BankQuestion> bank() {
        return BANK.stream();
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
            assertTrue(result.explanation.toHyperText(Language.RUSSIAN).getText().isBlank());
        }
    }

    /** Переход по вычисленному условию засчитывает умение определять его значение. */
    @ParameterizedTest
    @MethodSource("bank")
    void judgeQuestionCreditsConditionValueAfterCondition(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);

        for (int step = 1; step < bankQuestion.steps(); step++) {
            // Act.
            var result = judge(question, trace(question, bankQuestion, step + 1));

            // Assert.
            assertEquals(bankQuestion.isCondition(step - 1), result.domainSkills.contains(CONDITION_VALUE_SKILL),
                    bankQuestion.file() + " на шаге " + step);
        }
    }

    /** Пропуск первого действия отклоняется как нарушение последовательности. */
    @ParameterizedTest
    @MethodSource("bank")
    void judgeQuestionRejectsSkippedFirstAction(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);

        // Act.
        var result = judge(question, List.of(action(question, bankQuestion.action(1))));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(bankQuestion.steps(), result.IterationsLeft);
        assertEquals(List.of(SEQUENTIAL_ORDER_SKILL), lawNames(result.violations));
        assertTrue(result.explanation.toHyperText(Language.RUSSIAN).getText().contains("сначала должно произойти"));
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
        assertEquals(List.of(REPEATED_ACTION_SKILL), lawNames(result.violations));
        assertTrue(result.explanation.toHyperText(Language.RUSSIAN).getText().contains("два раза подряд"));
    }

    /** Возврат к давно выполненному действию отклоняется. */
    @Test
    void judgeQuestionRejectsReturnToExecutedAction() {
        // Arrange.
        var question = bankQuestion(WHILE_NOT_ENTERED);

        // Act.
        var result = judge(question, withMistake(question, WHILE_NOT_ENTERED, 4, "atom_111"));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(List.of(SEQUENTIAL_ORDER_SKILL), lawNames(result.violations));
        assertTrue(result.explanation.toHyperText(Language.RUSSIAN).getText().contains("не должно выполняться повторно"));
    }

    /** Действие после цикла без вычисления его условия отклоняется. */
    @Test
    void judgeQuestionRejectsSkippedCondition() {
        // Arrange.
        var question = bankQuestion(WHILE_NOT_ENTERED);

        // Act.
        var result = judge(question, withMistake(question, WHILE_NOT_ENTERED, 3, "atom_140"));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(2, result.IterationsLeft);
        assertEquals(List.of(SEQUENTIAL_ORDER_SKILL), lawNames(result.violations));
        assertTrue(result.explanation.toHyperText(Language.RUSSIAN).getText().contains("должно быть вычислено"));
        assertTrue(result.explanation.toHyperText(Language.RUSSIAN).getText().contains("s < n"));
    }

    /** Тело цикла при ложном условии отклоняется. */
    @Test
    void judgeQuestionRejectsLoopBodyWhenConditionIsFalse() {
        // Arrange.
        var question = bankQuestion(WHILE_NOT_ENTERED);

        // Act.
        var result = judge(question, withMistake(question, WHILE_NOT_ENTERED, 4, "atom_124"));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(1, result.IterationsLeft);
        assertEquals(List.of(CONDITION_TRANSITION_SKILL), lawNames(result.violations));
        assertTrue(result.explanation.toHyperText(Language.RUSSIAN).getText().contains("равно ложь"));
    }

    /** Ветка then при ложном условии отклоняется. */
    @Test
    void judgeQuestionRejectsThenBranchWhenConditionIsFalse() {
        // Arrange.
        var question = bankQuestion(IF_ELIF);

        // Act.
        var result = judge(question, withMistake(question, IF_ELIF, 3, "atom_120"));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(List.of(CONDITION_TRANSITION_SKILL), lawNames(result.violations));
    }

    /** Ветка else при истинном условии отклоняется. */
    @Test
    void judgeQuestionRejectsElseBranchWhenConditionIsTrue() {
        // Arrange.
        var question = bankQuestion(IF_ELIF);

        // Act.
        var result = judge(question, withMistake(question, IF_ELIF, 4, "atom_144"));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(List.of(CONDITION_TRANSITION_SKILL), lawNames(result.violations));
        assertTrue(result.explanation.toHyperText(Language.RUSSIAN).getText().contains("равно истина"));
    }

    /** Ветка уже отвергнутого условия отклоняется как далёкая от точки выполнения. */
    @Test
    void judgeQuestionRejectsBranchOfRejectedCondition() {
        // Arrange.
        var question = bankQuestion(IF_ELIF);

        // Act.
        var result = judge(question, withMistake(question, IF_ELIF, 4, "atom_120"));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(List.of(FAR_AWAY_ACTION_SKILL), lawNames(result.violations));
        assertTrue(result.explanation.toHyperText(Language.RUSSIAN).getText().contains("слишком далеко"));
    }

    /** Выход из цикла после тела без повторной проверки условия отклоняется. */
    @Test
    void judgeQuestionRequiresLoopConditionAfterBody() {
        // Arrange.
        var question = bankQuestion(WHILE_ONE_ITERATION);

        // Act.
        var result = judge(question, withMistake(question, WHILE_ONE_ITERATION, 6, "atom_163"));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(2, result.IterationsLeft);
        assertEquals(List.of(SEQUENTIAL_ORDER_SKILL), lawNames(result.violations));
        assertTrue(result.explanation.toHyperText(Language.RUSSIAN).getText().contains("x > 5"));
    }

    /** Новая итерация после ложного условия цикла отклоняется. */
    @Test
    void judgeQuestionRejectsIterationAfterFalseLoopCondition() {
        // Arrange.
        var question = bankQuestion(WHILE_ONE_ITERATION);

        // Act.
        var result = judge(question, withMistake(question, WHILE_ONE_ITERATION, 7, "atom_123"));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(1, result.IterationsLeft);
        assertEquals(List.of(CONDITION_TRANSITION_SKILL), lawNames(result.violations));
    }

    /** Выход из цикла по break засчитывает завершение прерывания. */
    @Test
    void judgeQuestionCreditsInterruptionExitAfterBreak() {
        // Arrange.
        var question = bankQuestion(BREAK_IN_FOR);

        // Act.
        var result = judge(question, trace(question, BREAK_IN_FOR));

        // Assert.
        assertTrue(result.isAnswerCorrect);
        assertEquals(0, result.IterationsLeft);
        assertTrue(result.domainSkills.contains(INTERRUPTION_TERMINATED_SKILL));
    }

    /** break раньше предшествующего действия тела отклоняется. */
    @Test
    void judgeQuestionRejectsBreakBeforePrecedingStatement() {
        // Arrange.
        var question = bankQuestion(BREAK_IN_FOR);

        // Act.
        var result = judge(question, withMistake(question, BREAK_IN_FOR, 10, "atom_140"));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(3, result.IterationsLeft);
        assertEquals(List.of(SEQUENTIAL_ORDER_SKILL), lawNames(result.violations));
    }

    /** Выход из цикла посреди итерации без break отклоняется. */
    @Test
    void judgeQuestionRejectsLoopExitWithoutBreak() {
        // Arrange.
        var question = bankQuestion(BREAK_IN_FOR);

        // Act.
        var result = judge(question, withMistake(question, BREAK_IN_FOR, 6, "atom_186"));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(7, result.IterationsLeft);
        assertTrue(lawNames(result.violations).contains(SEQUENTIAL_ORDER_SKILL));
        assertTrue(lawNames(result.violations).contains(INTERRUPTION_TRANSITION_SKILL));
    }

    /** Продолжение цикла после break отклоняется как переход, недопустимый при прерывании. */
    @ParameterizedTest
    @ValueSource(strings = {"atom_121", "atom_131", "atom_137"})
    void judgeQuestionRejectsLoopContinuationAfterBreak(String loopAction) {
        // Arrange.
        var question = bankQuestion(BREAK_IN_FOR);

        // Act.
        var result = judge(question, withMistake(question, BREAK_IN_FOR, 12, loopAction));

        // Assert.
        assertFalse(result.isAnswerCorrect);
        assertEquals(1, result.IterationsLeft);
        assertEquals(List.of(INTERRUPTION_MODE_SKILL), lawNames(result.violations));
        assertTrue(result.explanation.toHyperText(Language.RUSSIAN).getText().contains("прерывание цикла"));
    }

    /** Объяснение ошибки локализовано. */
    @Test
    void judgeQuestionExplainsMistakeInRequestedLanguage() {
        // Arrange.
        var question = bankQuestion(SEQUENCE);
        var wrong = responses(action(question, "atom_107"));

        // Act.
        var english = domain().judgeQuestion(question, wrong, List.of(), Language.ENGLISH).explanation.toHyperText(Language.ENGLISH).getText();
        var russian = domain().judgeQuestion(question, wrong, List.of(), Language.RUSSIAN).explanation.toHyperText(Language.RUSSIAN).getText();

        // Assert.
        assertFalse(english.isBlank());
        assertFalse(CYRILLIC.matcher(english).find(), english);
        assertFalse(english.contains("null"), english);
        assertTrue(english.contains("<code>str_: str = \"h\"</code>"), english);
        assertNotEquals(english, russian);
    }

    /** Вопросы других версий не оцениваются. */
    @Test
    void judgeQuestionRejectsUnsupportedQuestionVersion() {
        // Arrange.
        var record = bankRecord(SEQUENCE);
        record.setVersion(1);
        var question = QuestionData.of(domain().makeQuestion(record, List.of(), Language.RUSSIAN).getContent());

        // Act & Assert.
        assertThrows(UnsupportedOperationException.class, () -> judge(question, List.of(action(question, "atom_104"))));
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
            assertEquals(1, hint.answers.size());
            assertEquals(expected, hint.answers.getFirst().getLeft());
            assertEquals(expected, hint.answers.getFirst().getRight());
            assertFalse(hint.skillName.isEmpty());
            assertNull(hint.lawName);
            given.add(expected);
        }
    }

    /** Ошибочная попытка на подсказку не влияет. */
    @Test
    void hintIgnoresMistakenInteraction() {
        // Arrange.
        var question = bankQuestion(SEQUENCE);
        var mistaken = question.withInteraction(interaction(1L, List.of(action(question, "atom_107")),
                List.of(violation(SEQUENTIAL_ORDER_SKILL)), SEQUENCE.steps()));

        // Act.
        var hint = domain().getAnyNextCorrectAnswer(mistaken, Language.RUSSIAN);

        // Assert.
        assertEquals("atom_104", hint.answers.getFirst().getLeft().getDomainInfo());
    }

    /** Подсказка о выходе из цикла объясняет завершение прерывания. */
    @Test
    void hintExplainsInterruptionExit() {
        // Arrange.
        var question = bankQuestion(BREAK_IN_FOR);
        var afterBreak = withCorrectSteps(question, trace(question, BREAK_IN_FOR, 12), BREAK_IN_FOR);

        // Act.
        var hint = domain().getAnyNextCorrectAnswer(afterBreak, Language.RUSSIAN);

        // Assert.
        assertEquals("atom_186", hint.answers.getFirst().getLeft().getDomainInfo());
        assertTrue(hint.skillName.contains(INTERRUPTION_TERMINATED_SKILL));
        assertTrue(hint.explanation.toHyperText(Language.RUSSIAN).getText().contains("прерывание цикла"));
    }

    // ---- getFullSolutionTrace ----

    /** Трасса решения: старт программы и все выполненные действия с условиями. */
    @ParameterizedTest
    @MethodSource("bank")
    void solutionTraceListsProgramStartAndExecutedActions(BankQuestion bankQuestion) {
        // Arrange.
        var question = bankQuestion(bankQuestion);
        var solved = withCorrectSteps(question, trace(question, bankQuestion), bankQuestion);

        // Act.
        var trace = domain().getFullSolutionTrace(solved, Language.RUSSIAN);

        // Assert.
        assertEquals(bankQuestion.steps() + 1, trace.size());
        assertTrue(trace.getFirst().getText().contains("программа"));
        assertTrue(trace.getFirst().getText().contains("начала свое выполнение"));
        for (int step = 0; step < bankQuestion.steps(); step++) {
            var line = trace.get(step + 1).getText();
            assertTrue(line.contains("1-й") || line.contains("2-й") || line.contains("3-й"), line);
            if (bankQuestion.isCondition(step)) {
                assertTrue(line.contains("вычислилось"), line);
                assertTrue(line.contains("истина") || line.contains("ложь"), line);
            } else {
                assertTrue(line.contains("выполнил"), line);
                assertFalse(line.contains("истина") || line.contains("ложь"), line);
            }
        }
    }

    /** Повторные вычисления условия нумеруются. */
    @Test
    void solutionTraceCountsRepeatedConditions() {
        // Arrange.
        var question = bankQuestion(BREAK_IN_FOR);
        var solved = withCorrectSteps(question, trace(question, BREAK_IN_FOR), BREAK_IN_FOR);

        // Act.
        var trace = domain().getFullSolutionTrace(solved, Language.RUSSIAN);

        // Assert.
        assertTrue(trace.get(5).getText().contains("1-й"));
        assertTrue(trace.get(7).getText().contains("2-й"));
        assertTrue(trace.get(9).getText().contains("3-й"));
        assertTrue(trace.get(10).getText().contains("3-й"));
        assertTrue(trace.get(11).getText().contains("1-й"));
    }

    /** До первого ответа трасса содержит только старт программы. */
    @Test
    void solutionTraceBeforeFirstAnswerHasOnlyProgramStart() {
        // Act.
        var trace = domain().getFullSolutionTrace(bankQuestion(SEQUENCE), Language.RUSSIAN);

        // Assert.
        assertEquals(1, trace.size());
        assertTrue(trace.getFirst().getText().contains("программа"));
    }

    /** Последняя ошибка попадает в трассу подсвеченной. */
    @Test
    void solutionTraceHighlightsLastMistake() {
        // Arrange.
        var question = bankQuestion(SEQUENCE);
        var afterMistake = question.withInteraction(interaction(1L, List.of(action(question, "atom_107")),
                List.of(violation(SEQUENTIAL_ORDER_SKILL)), SEQUENCE.steps()));
        var afterCorrect = question.withInteraction(interaction(2L, List.of(action(question, "atom_104")), List.of(), 1));

        // Act.
        var mistaken = domain().getFullSolutionTrace(afterMistake, Language.RUSSIAN);
        var correct = domain().getFullSolutionTrace(afterCorrect, Language.RUSSIAN);

        // Assert.
        assertEquals(2, mistaken.size());
        assertTrue(mistaken.getLast().getText().startsWith("<span class=\"warning\">"));
        assertEquals(2, correct.size());
        assertFalse(correct.getLast().getText().contains("warning"));
    }

    /** Трасса локализована. */
    @Test
    void solutionTraceIsLocalized() {
        // Arrange.
        var question = bankQuestion(SEQUENCE);
        var solved = withCorrectSteps(question, trace(question, SEQUENCE), SEQUENCE);

        // Act.
        var english = domain().getFullSolutionTrace(solved, Language.ENGLISH);
        var russian = domain().getFullSolutionTrace(solved, Language.RUSSIAN);

        // Assert.
        assertEquals(russian.size(), english.size());
        assertTrue(english.getLast().getText().contains("executed"));
        assertFalse(english.getLast().getText().contains("null"));
        assertTrue(english.getLast().getText().contains("str_"));
    }

    // ---- вспомогательное ----

    private static Domain.InterpretSentenceResult judge(QuestionData question, List<AnswerObjectData> answers) {
        return domain().judgeQuestion(question, responses(answers), List.of(), Language.RUSSIAN);
    }

    private static List<AnswerObjectData> withMistake(QuestionData question, BankQuestion bankQuestion, int correctSteps, String wrongAction) {
        var given = new ArrayList<>(trace(question, bankQuestion, correctSteps));
        given.add(action(question, wrongAction));
        return given;
    }

    private static List<String> lawNames(List<ViolationData> violations) {
        return violations.stream().map(ViolationData::getLawName).toList();
    }

    private static QuestionData withCorrectSteps(QuestionData question, List<AnswerObjectData> given, BankQuestion bankQuestion) {
        return DomainFixtures.withCorrectSteps(question, given, bankQuestion.steps());
    }
}
