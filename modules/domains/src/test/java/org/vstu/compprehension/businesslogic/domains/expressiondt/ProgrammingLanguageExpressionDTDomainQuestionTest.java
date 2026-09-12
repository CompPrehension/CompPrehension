package org.vstu.compprehension.businesslogic.domains;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.vstu.compprehension.businesslogic.domains.ExpressionDtDomainFixture.BankQuestion;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.questionoptions.OrderQuestionOptionsData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.QuestionType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.vstu.compprehension.businesslogic.domains.ExpressionDtDomainFixture.BANK;
import static org.vstu.compprehension.businesslogic.domains.ExpressionDtDomainFixture.CPP_TAG;
import static org.vstu.compprehension.businesslogic.domains.ExpressionDtDomainFixture.END_TOKEN;
import static org.vstu.compprehension.businesslogic.domains.ExpressionDtDomainFixture.MEMBER_ACCESS_PLUS;
import static org.vstu.compprehension.businesslogic.domains.ExpressionDtDomainFixture.bankRecord;
import static org.vstu.compprehension.businesslogic.domains.ExpressionDtDomainFixture.cppTags;
import static org.vstu.compprehension.businesslogic.domains.ExpressionDtDomainFixture.domain;

class ProgrammingLanguageExpressionDTDomainQuestionTest {

    private static final String ORDER_OPERATORS = "OrderOperators";
    private static final String OPERATOR_CONCEPT = "operator";
    private static final String END_EVALUATION_CONCEPT = "student_end_evaluation";

    static Stream<BankQuestion> bank() {
        return BANK.stream();
    }

    /** Вопрос из банка: тип, метаданные и теги. */
    @ParameterizedTest
    @MethodSource("bank")
    void makeQuestionKeepsBankIdentity(BankQuestion bankQuestion) {
        // Arrange.
        var record = bankRecord(bankQuestion);

        // Act.
        var content = domain().makeQuestion(record, cppTags(), Language.ENGLISH).getContent();

        // Assert.
        assertEquals(QuestionType.ORDER, content.getQuestionType());
        assertEquals(ORDER_OPERATORS, content.getQuestionDomainType());
        assertEquals(domain().getDomainId(), content.getDomainId());
        assertEquals(record.getId(), content.getMetadata().getId());
        assertEquals(record.getName(), content.getMetadata().getName());
        assertEquals(record.getSolutionSteps(), content.getMetadata().getSolutionSteps());
        assertEquals(List.of(CPP_TAG), content.getTags());
        assertFalse(content.getStatementFacts().isEmpty());
    }

    /** Объекты ответа: операторы по порядку в выражении и «всё вычислено» последним. */
    @ParameterizedTest
    @MethodSource("bank")
    void makeQuestionBuildsAnswerObjectsForOperators(BankQuestion bankQuestion) {
        // Act.
        var answers = domain().makeQuestion(bankRecord(bankQuestion), cppTags(), Language.ENGLISH).getContent().getAnswerObjects();

        // Assert.
        assertEquals(bankQuestion.steps() + 1, answers.size());
        for (int i = 0; i < answers.size(); i++) {
            assertEquals(i, answers.get(i).getAnswerId());
            assertFalse(answers.get(i).isRightCol());
        }
        var operators = answers.subList(0, bankQuestion.steps());
        assertEquals(Set.copyOf(bankQuestion.evaluationOrder()), operators.stream().map(AnswerObjectData::getHyperText).collect(Collectors.toSet()));
        assertTrue(operators.stream().allMatch(a -> a.getDomainInfo().startsWith("token_")));
        assertTrue(operators.stream().allMatch(a -> OPERATOR_CONCEPT.equals(a.getConcept())));
        assertEquals(tokenIndexes(operators), tokenIndexes(operators).stream().sorted().toList());
        var end = answers.getLast();
        assertEquals(END_TOKEN, end.getDomainInfo());
        assertEquals(END_EVALUATION_CONCEPT, end.getConcept());
    }

    /** Текст вопроса содержит выражение и локализованную подсказку. */
    @Test
    void makeQuestionRendersLocalizedText() {
        // Act.
        var english = domain().makeQuestion(bankRecord(MEMBER_ACCESS_PLUS), cppTags(), Language.ENGLISH).getContent().getQuestionText();
        var russian = domain().makeQuestion(bankRecord(MEMBER_ACCESS_PLUS), cppTags(), Language.RUSSIAN).getContent().getQuestionText();

        // Assert.
        assertTrue(english.contains("Press the operators in the expression in the order they are evaluated"));
        assertTrue(english.contains("wp"));
        assertTrue(english.contains("sb_w"));
        assertTrue(english.contains("everything is evaluated"));
        assertNotEquals(english, russian);
        assertTrue(russian.contains("sb_w"));
    }

    /** Настройки вопроса на порядок: с контекстом и трассой, без множественного выбора. */
    @Test
    void makeQuestionSetsOrderOptions() {
        // Act.
        var options = domain().makeQuestion(bankRecord(MEMBER_ACCESS_PLUS), cppTags(), Language.ENGLISH).getContent().getOptions();

        // Assert.
        var orderOptions = assertInstanceOf(OrderQuestionOptionsData.class, options);
        assertTrue(orderOptions.isRequireContext());
        assertTrue(orderOptions.isShowTrace());
        assertFalse(orderOptions.isMultipleSelectionEnabled());
    }

    private static List<Integer> tokenIndexes(List<AnswerObjectData> operators) {
        return operators.stream().map(a -> Integer.parseInt(a.getDomainInfo().substring("token_".length()))).toList();
    }
}
