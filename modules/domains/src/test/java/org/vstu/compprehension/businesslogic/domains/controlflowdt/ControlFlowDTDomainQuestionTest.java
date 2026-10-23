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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.BANK;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.PLAY;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.QUESTION;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.SEQUENCE;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.bankRecord;
import static org.vstu.compprehension.businesslogic.domains.controlflowdt.ControlFlowDtDomainFixture.domain;

class ControlFlowDTDomainQuestionTest {

    private static final String ORDER_ACTS = "OrderActs";
    private static final String ACTION_CONCEPT = "action";
    private static final String LOQI_FACT = "hasLoqi";
    private static final String PYTHON_TAG = "python";

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
        assertEquals(2, content.getMetadata().getVersion());
        assertEquals(bankQuestion.steps() + 1, content.getMetadata().getSolutionSteps());
        assertEquals(List.of(PYTHON_TAG), content.getTags());
        assertEquals(1, content.getStatementFacts().size());
        assertEquals(LOQI_FACT, content.getStatementFacts().getFirst().getVerb());
        assertTrue(content.getStatementFacts().getFirst().getObject().contains("var STATE"));
    }

    /** Объекты ответа: кнопки действий и условий с уникальными узлами графа. */
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
        assertTrue(Set.of(PLAY, QUESTION).containsAll(hyperTexts(answers)));
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
        assertTrue(english.startsWith("<p>Press the actions of the algorithm in the order they are evaluated"));
        assertTrue(russian.startsWith("<p>Нажмите на действия алгоритма в том порядке, в котором они выполнятся"));
        assertTrue(english.contains("str_"));
        assertTrue(english.contains("id=\"answer_0\""));
        assertNotEquals(english, russian);
        assertEquals(english.substring(english.indexOf("<div")), russian.substring(russian.indexOf("<div")));
    }

    /** Настройки вопроса на порядок: с трассой, контекстом и повторным выбором действий. */
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
    }

    private static Set<String> hyperTexts(List<AnswerObjectData> answers) {
        return answers.stream().map(AnswerObjectData::getHyperText).collect(Collectors.toSet());
    }
}
