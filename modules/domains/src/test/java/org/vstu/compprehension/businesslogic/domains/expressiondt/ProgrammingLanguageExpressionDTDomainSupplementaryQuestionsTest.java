package org.vstu.compprehension.businesslogic.domains.expressiondt;

import org.junit.jupiter.api.Test;
import org.vstu.compprehension.businesslogic.SupplementaryStepContext;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.NewSupplementaryStepData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.SupplementaryStepData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.QuestionType;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.MEMBER_ACCESS_PLUS;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.PARENTHESES_AND_UNARY_MINUS;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.bankQuestion;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.domain;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.endToken;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.operator;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.operators;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.responses;

class ProgrammingLanguageExpressionDTDomainSupplementaryQuestionsTest {

    private static final long INTERACTION_ID = 1L;
    private static final Language LANGUAGE = Language.RUSSIAN;

    // ---- ошибка порядка операторов ----

    /** После ошибки приоритета — вопрос на сопоставление. */
    @Test
    void precedenceMistakeStartsWithMatchingQuestion() {
        // Arrange.
        var question = bankQuestion(PARENTHESES_AND_UNARY_MINUS);
        var mistake = interaction(question, operator(question, "&"));

        // Act.
        var response = domain().makeSupplementaryQuestion(question.withInteraction(mistake), null, new ViolationData(), LANGUAGE);

        // Assert.
        var supplementary = response.getResponse().getQuestion();
        assertNotNull(supplementary);
        assertNull(response.getResponse().getFeedback());
        assertNotNull(response.getNewStep());
        assertEquals(QuestionType.MATCHING, supplementary.getContent().getQuestionType());
        assertFalse(options(supplementary.getContent().getAnswerObjects()).isEmpty());
        assertFalse(groups(supplementary.getContent().getAnswerObjects()).isEmpty());
    }

    /** Неверное сопоставление — ошибка с паузой. */
    @Test
    void wrongMatchingIsRejected() {
        // Arrange.
        var question = bankQuestion(PARENTHESES_AND_UNARY_MINUS);
        var mistake = interaction(question, operator(question, "&"));
        var main = question.withInteraction(mistake);
        var first = domain().makeSupplementaryQuestion(main, null, new ViolationData(), LANGUAGE);
        var supplementary = first.getResponse().getQuestion().getContent().getAnswerObjects();

        // Act.
        var feedback = domain().judgeSupplementaryQuestion(main, stepContext(mistake, first.getNewStep()),
                everythingToFirstGroup(supplementary), LANGUAGE);

        // Assert.
        assertEquals(FeedbackDto.MessageType.ERROR, feedback.getFeedback().getMessage().getType());
        assertEquals(SupplementaryFeedbackDto.Action.ContinueManual, feedback.getFeedback().getAction());
        assertNotNull(feedback.getNewStep());
    }

    /** После отвеченного шага цепочка продолжается сообщением. */
    @Test
    void chainContinuesAfterAnsweredMatching() {
        // Arrange.
        var question = bankQuestion(PARENTHESES_AND_UNARY_MINUS);
        var mistake = interaction(question, operator(question, "&"));
        var main = question.withInteraction(mistake);
        var first = domain().makeSupplementaryQuestion(main, null, new ViolationData(), LANGUAGE);
        var answered = domain().judgeSupplementaryQuestion(main, stepContext(mistake, first.getNewStep()),
                everythingToFirstGroup(first.getResponse().getQuestion().getContent().getAnswerObjects()), LANGUAGE);

        // Act.
        var next = domain().makeSupplementaryQuestion(main, stepData(mistake, answered.getNewStep()), new ViolationData(), LANGUAGE);

        // Assert.
        assertNull(next.getResponse().getQuestion());
        var feedback = next.getResponse().getFeedback();
        assertNotNull(feedback);
        assertEquals(FeedbackDto.MessageType.SUCCESS, feedback.getMessage().getType());
        assertEquals(SupplementaryFeedbackDto.Action.ContinueAuto, feedback.getAction());
    }

    // ---- преждевременное «всё вычислено» ----

    /** После раннего финиша — выбор всех невычисленных операторов. */
    @Test
    void earlyFinishStartsWithUnevaluatedOperatorsChoice() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));

        // Act.
        var response = domain().makeSupplementaryQuestion(question.withInteraction(mistake), null, new ViolationData(), LANGUAGE);

        // Assert.
        var supplementary = response.getResponse().getQuestion();
        assertNotNull(supplementary);
        assertEquals(QuestionType.MULTI_CHOICE, supplementary.getContent().getQuestionType());
        assertEquals(operators(question).size(), options(supplementary.getContent().getAnswerObjects()).size());
    }

    /** Неполный список невычисленных операторов — ошибка. */
    @Test
    void partialSelectionOfUnevaluatedOperatorsIsRejected() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));
        var main = question.withInteraction(mistake);
        var first = domain().makeSupplementaryQuestion(main, null, new ViolationData(), LANGUAGE);
        var options = options(first.getResponse().getQuestion().getContent().getAnswerObjects());

        // Act.
        var feedback = domain().judgeSupplementaryQuestion(main, stepContext(mistake, first.getNewStep()),
                chosen(options.subList(0, 1)), LANGUAGE);

        // Assert.
        assertEquals(FeedbackDto.MessageType.ERROR, feedback.getFeedback().getMessage().getType());
        assertEquals(SupplementaryFeedbackDto.Action.ContinueManual, feedback.getFeedback().getAction());
    }

    /** Все невычисленные операторы найдены — дальше следующий вопрос. */
    @Test
    void fullSelectionOfUnevaluatedOperatorsContinuesChain() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));
        var main = question.withInteraction(mistake);
        var first = domain().makeSupplementaryQuestion(main, null, new ViolationData(), LANGUAGE);
        var options = options(first.getResponse().getQuestion().getContent().getAnswerObjects());

        // Act.
        var feedback = domain().judgeSupplementaryQuestion(main, stepContext(mistake, first.getNewStep()), chosen(options), LANGUAGE);
        var next = domain().makeSupplementaryQuestion(main, stepData(mistake, feedback.getNewStep()), new ViolationData(), LANGUAGE);

        // Assert.
        assertEquals(FeedbackDto.MessageType.SUCCESS, feedback.getFeedback().getMessage().getType());
        assertEquals(SupplementaryFeedbackDto.Action.ContinueAuto, feedback.getFeedback().getAction());
        assertNotNull(next.getResponse().getQuestion());
        assertNull(next.getResponse().getFeedback());
    }

    // ---- вспомогательное ----

    private static QuestionInteractionData interaction(QuestionData question, AnswerObjectData... answers) {
        var given = responses(answers);
        var tags = domain().resolveTags(question.getContent().getTags());
        assertFalse(domain().judgeQuestion(question, given, tags, LANGUAGE).isAnswerCorrect);
        return QuestionInteractionData.builder()
                .id(INTERACTION_ID)
                .interactionType(InteractionType.SEND_RESPONSE)
                .responses(given)
                .build();
    }

    private static SupplementaryStepData stepData(QuestionInteractionData interaction, NewSupplementaryStepData newStep) {
        return SupplementaryStepData.builder()
                .mainQuestionInteractionId(interaction.getId())
                .situationInfo(newStep.getSituationInfo())
                .nextStateId(newStep.getNextStateId())
                .build();
    }

    private static SupplementaryStepContext stepContext(QuestionInteractionData interaction, NewSupplementaryStepData newStep) {
        return new SupplementaryStepContext(stepData(interaction, newStep), interaction);
    }

    private static List<AnswerObjectData> options(List<AnswerObjectData> answerObjects) {
        return answerObjects.stream().filter(a -> !a.isRightCol()).toList();
    }

    private static List<AnswerObjectData> groups(List<AnswerObjectData> answerObjects) {
        return answerObjects.stream().filter(AnswerObjectData::isRightCol).toList();
    }

    private static List<AnswerData> chosen(List<AnswerObjectData> options) {
        return options.stream().map(a -> AnswerData.of(a, a)).toList();
    }

    private static List<AnswerData> everythingToFirstGroup(List<AnswerObjectData> answerObjects) {
        var group = groups(answerObjects).getFirst();
        return options(answerObjects).stream().map(a -> AnswerData.of(a, group)).toList();
    }
}
