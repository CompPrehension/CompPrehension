package org.vstu.compprehension.businesslogic.domains.controlflowdt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.BankQuestion;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.questionoptions.OrderQuestionOptionsData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.QuestionType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.BANK;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.PLAY;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.QUESTION;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.SEQUENCE;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.STEP_INTO;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.STEP_OUT;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.bankRecord;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.domain;

class ControlFlowDTDomainQuestionTest {

    private static final String ORDER_ACTS = "OrderActs";
    private static final String ACTION_CONCEPT = "action";
    private static final String PYTHON_TAG = "python";
    private static final String CONCRETE_ACTION_CLASS = "ConcreteAction";
    private static final String TRACE_STATE_VARIABLE = "S";
    private static final String TRACE_STATE_CLASS = "TraceState";
    private static final String SEMANTIC_VALUE_CLASS = "SemanticValue";

    static Stream<BankQuestion> bank() {
        return BANK.stream();
    }

    /** Вопрос из банка: тип, метаданные, теги и модель ситуации. */
    @ParameterizedTest
    @MethodSource("bank")
    void makeQuestionKeepsBankIdentity(BankQuestion bankQuestion) {
        // Arrange.
        var record = bankRecord(bankQuestion);

        // Act.
        var content = domain().makeQuestion(record, List.of(), Language.ENGLISH).getContent();

        // Assert.
        assertEquals(QuestionType.ORDER, content.getQuestionType());
        assertEquals(ORDER_ACTS, content.getQuestionDomainType());
        assertEquals(domain().getDomainId(), content.getDomainId());
        assertEquals(record.getId(), content.getMetadata().getId());
        assertEquals(record.getName(), content.getMetadata().getName());
        assertEquals(3, content.getMetadata().getVersion());
        assertEquals(bankQuestion.steps(), content.getMetadata().getSolutionSteps());
        assertEquals(List.of(PYTHON_TAG), content.getTags());
    }

    /** Условие вопроса разворачивается в модель ситуации: в ней есть действия и значения всех ответов и состояние трассы. */
    @ParameterizedTest
    @MethodSource("bank")
    void questionModelHoldsAnswerActionsAndTraceState(BankQuestion bankQuestion) {
        // Arrange.
        var content = domain().makeQuestion(bankRecord(bankQuestion), List.of(), Language.ENGLISH).getContent();

        // Act.
        var model = domain().prepareQuestionModel(content);

        // Assert.
        for (AnswerObjectData answer : content.getAnswerObjects()) {
            var actionAndValue = answer.getDomainInfo().split(",", 2);
            var action = model.getObjects().get(actionAndValue[0]);
            assertNotNull(action, answer.getDomainInfo());
            assertTrue(action.isInstanceOf(CONCRETE_ACTION_CLASS), answer.getDomainInfo());
            if (actionAndValue.length > 1) {
                var value = model.getObjects().get(actionAndValue[1]);
                assertNotNull(value, answer.getDomainInfo());
                assertTrue(value.isInstanceOf(SEMANTIC_VALUE_CLASS), answer.getDomainInfo());
            }
        }
        assertTrue(model.getVariables().get(TRACE_STATE_VARIABLE).getValueObject().isInstanceOf(TRACE_STATE_CLASS));
    }

    /**
     * Объекты ответа с уникальными узлами графа: кнопки действий, условий и вызовов функций
     * и пункты dropdown завершения вызова.
     */
    @ParameterizedTest
    @MethodSource("bank")
    void makeQuestionBuildsAnswerObjectsForActions(BankQuestion bankQuestion) {
        // Act.
        var answers = domain().makeQuestion(bankRecord(bankQuestion), List.of(), Language.ENGLISH).getContent().getAnswerObjects();

        // Assert.
        var nodes = answers.stream().map(AnswerObjectData::getDomainInfo).collect(Collectors.toSet());
        assertEquals(answers.size(), nodes.size());
        assertTrue(nodes.containsAll(bankQuestion.trace()));
        assertEquals(IntStream.range(0, answers.size()).boxed().collect(Collectors.toSet()),
                answers.stream().map(AnswerObjectData::getAnswerId).collect(Collectors.toSet()));
        assertTrue(answers.stream().allMatch(a -> ACTION_CONCEPT.equals(a.getConcept())));
        assertTrue(answers.stream().noneMatch(AnswerObjectData::isRightCol));
        var buttons = answers.stream().filter(a -> !a.getDomainInfo().contains(",")).toList();
        assertTrue(Set.of(PLAY, QUESTION, STEP_INTO, STEP_OUT).containsAll(hyperTexts(buttons)));
        var callEnds = buttons.stream()
                .filter(a -> STEP_OUT.equals(a.getHyperText()))
                .map(AnswerObjectData::getDomainInfo)
                .collect(Collectors.toSet());
        assertTrue(answers.stream()
                .filter(a -> a.getDomainInfo().contains(","))
                .allMatch(a -> callEnds.contains(a.getDomainInfo().split(",", 2)[0])));
        assertEquals(bankQuestion.conditions(), answers.stream()
                .filter(a -> QUESTION.equals(a.getHyperText()))
                .map(AnswerObjectData::getDomainInfo)
                .collect(Collectors.toSet()));
    }

    /** Текст вопроса начинается с локализованной подсказки и содержит код. */
    @Test
    void makeQuestionPrefixesLocalizedPrompt() {
        // Act.
        var english = domain().makeQuestion(bankRecord(SEQUENCE), List.of(), Language.ENGLISH).getContent().getQuestionText();
        var russian = domain().makeQuestion(bankRecord(SEQUENCE), List.of(), Language.RUSSIAN).getContent().getQuestionText();

        // Assert.
        assertTrue(english.startsWith("<p>Press the action buttons in the order the actions are executed"));
        assertTrue(russian.startsWith("<p>Нажимайте кнопки действий в том порядке, в котором они выполнятся"));
        assertTrue(english.contains("str_"));
        assertTrue(english.contains("id=\"answer_0\""));
        assertNotEquals(english, russian);
        assertEquals(english.substring(english.indexOf("<div")), russian.substring(russian.indexOf("<div")));
    }

    /** Настройки вопроса на порядок: с трассой, контекстом, повторным выбором действий и без номеров шагов на кнопках. */
    @Test
    void makeQuestionSetsOrderOptions() {
        // Act.
        var options = domain().makeQuestion(bankRecord(SEQUENCE), List.of(), Language.ENGLISH).getContent().getOptions();

        // Assert.
        var orderOptions = assertInstanceOf(OrderQuestionOptionsData.class, options);
        assertTrue(orderOptions.isRequireContext());
        assertTrue(orderOptions.isShowTrace());
        assertTrue(orderOptions.isMultipleSelectionEnabled());
        assertTrue(orderOptions.isRequireAllAnswers());
        assertFalse(orderOptions.isShowSupplementaryQuestions());
        assertEquals(OrderQuestionOptionsData.OrderNumberPosition.NONE, orderOptions.getOrderNumberOptions().getPosition());
    }

    private static Set<String> hyperTexts(List<AnswerObjectData> answers) {
        return answers.stream().map(AnswerObjectData::getHyperText).collect(Collectors.toSet());
    }
}
