package org.vstu.compprehension.businesslogic.domains.expressiondt;

import org.junit.jupiter.api.Test;
import org.vstu.compprehension.businesslogic.SupplementaryResponse;
import org.vstu.compprehension.businesslogic.SupplementaryResponseGenerationResult;
import org.vstu.compprehension.businesslogic.SupplementaryStepContext;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.GeneratedQuestionData;
import org.vstu.compprehension.data.question.NewSupplementaryStepData;
import org.vstu.compprehension.data.question.QuestionContentData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.SupplementaryStepData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.data.questionoptions.MultiChoiceOptionsData;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.QuestionType;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.MEMBER_ACCESS_PLUS;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.MUL_PLUS_MINUS;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.PARENTHESES_AND_UNARY_MINUS;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.bankQuestion;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.domain;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.endToken;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.operator;
import static org.vstu.compprehension.businesslogic.domains.expressiondt.ExpressionDtDomainFixture.operators;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.answers;
import static org.vstu.compprehension.businesslogic.domains.DomainFixtures.responses;

class ProgrammingLanguageExpressionDTDomainSupplementaryQuestionsTest {

    private static final long INTERACTION_ID = 1L;
    private static final Language LANGUAGE = Language.RUSSIAN;
    private static final int MAX_DISCUSSION_TURNS = 30;
    private static final Pattern LATIN = Pattern.compile("[A-Za-z]");
    private static final Pattern CODE = Pattern.compile("<code>.*?</code>");

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
        var supplementary = question(response);
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
        var supplementary = question(first).getContent().getAnswerObjects();

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
                everythingToFirstGroup(question(first).getContent().getAnswerObjects()), LANGUAGE);

        // Act.
        var next = domain().makeSupplementaryQuestion(main, stepData(mistake, answered.getNewStep()), new ViolationData(), LANGUAGE);

        // Assert.
        var feedback = feedback(next);
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
        var supplementary = question(response);
        assertEquals(QuestionType.MULTI_CHOICE, supplementary.getContent().getQuestionType());
        assertEquals(operators(question).size(), options(supplementary.getContent().getAnswerObjects()).size());
    }

    /** Уже вычисленный оператор предлагается среди невычисленных, и его выбор объясняется. */
    @Test
    void evaluatedOperatorIsOfferedAmongUnevaluated() {
        // Arrange.
        var question = bankQuestion(MUL_PLUS_MINUS);
        var mistake = interaction(question, operator(question, "*"), endToken(question));
        var main = question.withInteraction(mistake);
        var first = domain().makeSupplementaryQuestion(main, null, new ViolationData(), LANGUAGE);
        var options = options(question(first).getContent().getAnswerObjects());

        // Act.
        var feedback = domain().judgeSupplementaryQuestion(main, stepContext(mistake, first.getNewStep()), switched(options, options), LANGUAGE);

        // Assert.
        assertEquals(operators(question).size(), options.size());
        assertEquals(FeedbackDto.MessageType.ERROR, feedback.getFeedback().getMessage().getType());
        assertEquals("Это неверно.\nОператор <code>*</code> на позиции 2 уже вычислен.", feedback.getFeedback().getMessage().getMessage());
    }

    /** Выбор только невычисленных операторов верен, даже когда среди вариантов есть вычисленный. */
    @Test
    void onlyUnevaluatedOperatorsSelectionIsAccepted() {
        // Arrange.
        var question = bankQuestion(MUL_PLUS_MINUS);
        var mistake = interaction(question, operator(question, "*"), endToken(question));
        var main = question.withInteraction(mistake);
        var first = domain().makeSupplementaryQuestion(main, null, new ViolationData(), LANGUAGE);
        var options = options(question(first).getContent().getAnswerObjects());
        var unevaluated = options.stream().filter(a -> !a.getHyperText().contains("<code>*</code>")).toList();

        // Act.
        var feedback = domain().judgeSupplementaryQuestion(main, stepContext(mistake, first.getNewStep()), switched(options, unevaluated), LANGUAGE);

        // Assert.
        assertEquals(FeedbackDto.MessageType.SUCCESS, feedback.getFeedback().getMessage().getType());
    }

    /** Оператор из операндов разбираемого оператора предлагается среди тех, в чьих операндах он находится, и его выбор объясняется. */
    @Test
    void operandOfDiscussedOperatorIsOfferedAmongItsParents() {
        // Arrange.
        var question = bankQuestion(MUL_PLUS_MINUS);
        var mistake = interaction(question, operator(question, "*"), endToken(question));

        // Act.
        var discussion = discussUntilTheEnd(question.withInteraction(mistake), mistake, List::getFirst, List::getFirst);

        // Assert.
        var parents = discussion.questions().stream()
                .filter(content -> content.getQuestionText().equals("Найдите все операторы, в операндах которых находится оператор <code>+</code> на позиции 4."))
                .findFirst().orElseThrow();
        var parentTexts = options(parents.getAnswerObjects()).stream().map(AnswerObjectData::getHyperText).collect(Collectors.toSet());
        assertEquals(Set.of("оператор <code>-</code> на позиции 6", "оператор <code>*</code> на позиции 2"), parentTexts);
        assertTrue(discussion.shownTexts().contains("Это неверно.\nОператор <code>*</code> на позиции 2 находится в операндах"
                + " оператора <code>+</code> на позиции 4, а не наоборот."), discussion.shownTexts().toString());
    }

    /** У верхнего оператора нет операторов, в чьих операндах он находится, поэтому вопрос о них не задаётся. */
    @Test
    void topOperatorParentsQuestionIsSkipped() {
        // Arrange.
        var question = bankQuestion(MUL_PLUS_MINUS);
        var mistake = interaction(question, operator(question, "*"), endToken(question));

        // Act.
        var discussion = discussUntilTheEnd(question.withInteraction(mistake), mistake, List::getLast, List::getFirst);

        // Assert.
        var texts = discussion.questions().stream().map(QuestionContentData::getQuestionText).toList();
        assertFalse(texts.contains("Найдите все операторы, в операндах которых находится оператор <code>-</code> на позиции 6."), texts.toString());
        assertTrue(texts.stream().anyMatch(text -> text.startsWith(
                "Учитывая, что оператор <code>-</code> на позиции 6 не находится в операндах других операторов")), texts.toString());
    }

    /** Варианты вопроса с множественным выбором показываются в случайном порядке. */
    @Test
    void multipleChoiceOptionsAreShuffled() {
        // Arrange.
        var question = bankQuestion(MUL_PLUS_MINUS);
        var mistake = interaction(question, operator(question, "*"), endToken(question));
        var main = question.withInteraction(mistake);

        // Act.
        var orders = new HashSet<List<Integer>>();
        for (int attempt = 0; attempt < 30; attempt++) {
            var first = domain().makeSupplementaryQuestion(main, null, new ViolationData(), LANGUAGE);
            orders.add(options(question(first).getContent().getAnswerObjects()).stream().map(AnswerObjectData::getAnswerId).toList());
        }

        // Assert.
        assertTrue(orders.size() > 1, orders.toString());
    }

    /** Неполный список невычисленных операторов — ошибка. */
    @Test
    void partialSelectionOfUnevaluatedOperatorsIsRejected() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));
        var main = question.withInteraction(mistake);
        var first = domain().makeSupplementaryQuestion(main, null, new ViolationData(), LANGUAGE);
        var options = options(question(first).getContent().getAnswerObjects());

        // Act.
        var feedback = domain().judgeSupplementaryQuestion(main, stepContext(mistake, first.getNewStep()),
                switched(options, options.subList(0, 1)), LANGUAGE);

        // Assert.
        assertEquals(FeedbackDto.MessageType.ERROR, feedback.getFeedback().getMessage().getType());
        assertEquals(SupplementaryFeedbackDto.Action.ContinueManual, feedback.getFeedback().getAction());
    }

    /** Пропущенный невычисленный оператор объясняется по-русски отдельным предложением. */
    @Test
    void missedUnevaluatedOperatorIsExplainedInRussian() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));
        var main = question.withInteraction(mistake);
        var first = domain().makeSupplementaryQuestion(main, null, new ViolationData(), LANGUAGE);
        var options = options(question(first).getContent().getAnswerObjects());

        // Act.
        var feedback = domain().judgeSupplementaryQuestion(main, stepContext(mistake, first.getNewStep()),
                switched(options, options.subList(0, 1)), LANGUAGE);

        // Assert.
        var message = feedback.getFeedback().getMessage().getMessage();
        assertTrue(message.contains("Оператор ") && message.contains(" тоже удовлетворяет условию."), message);
        assertFalse(message.contains("fits the criteria"), message);
    }

    /** Все невычисленные операторы найдены — дальше следующий вопрос. */
    @Test
    void fullSelectionOfUnevaluatedOperatorsContinuesChain() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));
        var main = question.withInteraction(mistake);
        var first = domain().makeSupplementaryQuestion(main, null, new ViolationData(), LANGUAGE);
        var options = options(question(first).getContent().getAnswerObjects());

        // Act.
        var feedback = domain().judgeSupplementaryQuestion(main, stepContext(mistake, first.getNewStep()), switched(options, options), LANGUAGE);
        var next = domain().makeSupplementaryQuestion(main, stepData(mistake, feedback.getNewStep()), new ViolationData(), LANGUAGE);

        // Assert.
        assertEquals(FeedbackDto.MessageType.SUCCESS, feedback.getFeedback().getMessage().getType());
        assertEquals(SupplementaryFeedbackDto.Action.ContinueAuto, feedback.getFeedback().getAction());
        question(next);
    }

    /** После выбора невычисленных операторов каждая строка сопоставления называет свой оператор. */
    @Test
    void unevaluatedOperatorsMatchingNamesEachOperator() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));
        var main = question.withInteraction(mistake);
        var first = domain().makeSupplementaryQuestion(main, null, new ViolationData(), LANGUAGE);
        var options = options(question(first).getContent().getAnswerObjects());
        var chosen = domain().judgeSupplementaryQuestion(main, stepContext(mistake, first.getNewStep()),
                switched(options, options), LANGUAGE);

        // Act.
        var next = domain().makeSupplementaryQuestion(main, stepData(mistake, chosen.getNewStep()), new ViolationData(), LANGUAGE);

        // Assert.
        var content = question(next).getContent();
        var statements = options(content.getAnswerObjects()).stream().map(AnswerObjectData::getHyperText).toList();
        assertEquals(QuestionType.MATCHING, content.getQuestionType());
        assertEquals(operators(question).size(), statements.size());
        assertEquals(statements.size(), new HashSet<>(statements).size(), statements.toString());
    }

    /** Разбор раннего финиша доходит до конца: каждый текст цепочки отрисовывается. */
    @Test
    void earlyFinishDiscussionRunsToTheEnd() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));

        // Act.
        var discussion = discussUntilTheEnd(question.withInteraction(mistake), mistake, List::getFirst, List::getFirst);

        // Assert.
        assertEquals(SupplementaryFeedbackDto.Action.Finish, discussion.last().getAction());
    }

    /** Верно сопоставленные невычисленные операторы не обрывают разбор: он доходит до конца. */
    @Test
    void correctlyMatchedEarlyFinishDiscussionRunsToTheEnd() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));

        // Act.
        var discussion = discussUntilTheEnd(question.withInteraction(mistake), mistake, List::getFirst, List::getLast);

        // Assert.
        assertEquals(SupplementaryFeedbackDto.Action.Finish, discussion.last().getAction());
    }

    /** Разбор верхнего оператора выражения не задаёт вопросов без вариантов ответа и доходит до конца. */
    @Test
    void topOperatorDiscussionAsksNoEmptyQuestions() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));

        // Act.
        var discussion = discussUntilTheEnd(question.withInteraction(mistake), mistake, List::getLast, List::getFirst);

        // Assert.
        var empty = discussion.questions().stream()
                .filter(content -> content.getAnswerObjects().isEmpty())
                .map(QuestionContentData::getQuestionText)
                .toList();
        assertEquals(List.of(), empty);
        assertEquals(SupplementaryFeedbackDto.Action.Finish, discussion.last().getAction());
    }

    /** Отзыв на последний ответ идёт отдельно, а следом — один свёрнутый итог вложенных ветвей и главной ветви, завершающий разбор. */
    @Test
    void earlyFinishEndsWithSingleSummaryAfterLastAnswer() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));

        // Act.
        var discussion = discussUntilTheEnd(question.withInteraction(mistake), mistake, List::getFirst, List::getFirst);

        // Assert.
        var answered = discussion.feedbacks().get(discussion.feedbacks().size() - 2);
        var message = discussion.last().getMessage().getMessage();
        assertFalse(discussion.finishedOnAnswer());
        assertFalse(answered.getMessage().getMessage().contains("Итак, мы обсудили"), answered.getMessage().getMessage());
        assertEquals(SupplementaryFeedbackDto.Action.ContinueManual, answered.getAction());
        assertEquals(SupplementaryFeedbackDto.Action.Finish, discussion.last().getAction());
        assertEquals(1, message.split("Итак, мы обсудили", -1).length - 1, message);
        assertTrue(message.endsWith("почему оператор <code>-></code> на позиции 2 мешает закончить вычисление выражения"
                + " и почему не все операторы в выражении вычислены полностью."), message);
    }

    /** Верный последний ответ — отдельное «Верно.» с автопереходом к завершающему итогу. */
    @Test
    void correctLastAnswerContinuesAutomaticallyToSummary() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));

        // Act.
        var discussion = discussUntilTheEnd(question.withInteraction(mistake), mistake, List::getLast, List::getFirst);

        // Assert.
        var answered = discussion.feedbacks().get(discussion.feedbacks().size() - 2);
        assertEquals("Верно.", answered.getMessage().getMessage());
        assertEquals(SupplementaryFeedbackDto.Action.ContinueAuto, answered.getAction());
        assertEquals(SupplementaryFeedbackDto.Action.Finish, discussion.last().getAction());
        assertEquals("Итак, мы обсудили, почему оператор <code>+</code> на позиции 4 мешает закончить вычисление выражения"
                + " и почему не все операторы в выражении вычислены полностью.", discussion.last().getMessage().getMessage());
    }

    /** Выбор причины при разборе оператора спрашивает, почему студент так считает, а не что применимо в ситуации. */
    @Test
    void operatorBranchReasonQuestionAsksWhyStudentThinksSo() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));

        // Act.
        var discussion = discussUntilTheEnd(question.withInteraction(mistake), mistake, List::getFirst, List::getFirst);

        // Assert.
        var texts = discussion.questions().stream().map(QuestionContentData::getQuestionText).toList();
        assertFalse(texts.contains("Что из перечисленного применимо в данной ситуации?"), texts.toString());
        assertTrue(texts.contains("Почему вы считаете, что оператор <code>-></code> на позиции 2 не требует вычисления"
                + " внутри оператора <code>+</code> на позиции 4?"), texts.toString());
    }

    /** Весь русский разбор раннего финиша написан по-русски: латиница встречается только в коде. */
    @Test
    void earlyFinishDiscussionHasNoLatinOutsideCode() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));

        // Act.
        var discussion = discussUntilTheEnd(question.withInteraction(mistake), mistake, List::getFirst, List::getFirst);

        // Assert.
        var withLatin = discussion.shownTexts().stream()
                .filter(text -> LATIN.matcher(withoutMarkup(text)).find())
                .toList();
        assertFalse(discussion.shownTexts().isEmpty());
        assertEquals(List.of(), withLatin);
    }

    /** Все переключатели выставлены в «нет» — это не полный выбор, а ошибка. */
    @Test
    void allSwitchesSetToNoAreRejected() {
        // Arrange.
        var question = bankQuestion(MEMBER_ACCESS_PLUS);
        var mistake = interaction(question, endToken(question));
        var main = question.withInteraction(mistake);
        var first = domain().makeSupplementaryQuestion(main, null, new ViolationData(), LANGUAGE);
        var options = options(question(first).getContent().getAnswerObjects());

        // Act.
        var feedback = domain().judgeSupplementaryQuestion(main, stepContext(mistake, first.getNewStep()),
                switched(options, List.of()), LANGUAGE);

        // Assert.
        assertEquals(FeedbackDto.MessageType.ERROR, feedback.getFeedback().getMessage().getType());
        assertEquals(SupplementaryFeedbackDto.Action.ContinueManual, feedback.getFeedback().getAction());
    }

    // ---- вспомогательное ----

    private static GeneratedQuestionData question(SupplementaryResponseGenerationResult result) {
        return assertInstanceOf(SupplementaryResponse.Question.class, result.getResponse()).question();
    }

    private static SupplementaryFeedbackDto feedback(SupplementaryResponseGenerationResult result) {
        return assertInstanceOf(SupplementaryResponse.Feedback.class, result.getResponse()).feedback();
    }

    private static QuestionInteractionData interaction(QuestionData question, AnswerObjectData... answers) {
        var given = responses(answers);
        var tags = domain().resolveTags(question.getContent().getTags());
        assertFalse(domain().judgeQuestion(question, answers(answers), tags, LANGUAGE).isAnswerCorrect);
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

    private static List<AnswerData> switched(List<AnswerObjectData> options, List<AnswerObjectData> chosen) {
        return options.stream()
                .<AnswerData>map(a -> new AnswerData.Choice(a, chosen.contains(a)
                        ? MultiChoiceOptionsData.SWITCH_ON
                        : MultiChoiceOptionsData.SWITCH_OFF))
                .toList();
    }

    private static List<AnswerData> everythingToFirstGroup(List<AnswerObjectData> answerObjects) {
        var group = groups(answerObjects).getFirst();
        return options(answerObjects).stream().<AnswerData>map(a -> new AnswerData.Pair(a, group)).toList();
    }

    private record Discussion(List<QuestionContentData> questions, List<String> shownTexts, List<SupplementaryFeedbackDto> feedbacks,
                              boolean finishedOnAnswer) {
        SupplementaryFeedbackDto last() {
            return feedbacks.getLast();
        }
    }

    private static Discussion discussUntilTheEnd(QuestionData main, QuestionInteractionData mistake,
                                                 Function<List<AnswerObjectData>, AnswerObjectData> singleChoice,
                                                 Function<List<AnswerObjectData>, AnswerObjectData> matchingGroup) {
        var questions = new ArrayList<QuestionContentData>();
        var shownTexts = new ArrayList<String>();
        var feedbacks = new ArrayList<SupplementaryFeedbackDto>();
        SupplementaryStepData step = null;
        for (int turn = 0; turn < MAX_DISCUSSION_TURNS; turn++) {
            var next = domain().makeSupplementaryQuestion(main, step, new ViolationData(), LANGUAGE);
            SupplementaryFeedbackDto feedback;
            NewSupplementaryStepData newStep;
            var onAnswer = !(next.getResponse() instanceof SupplementaryResponse.Feedback);
            if (next.getResponse() instanceof SupplementaryResponse.Feedback message) {
                feedback = message.feedback();
                newStep = next.getNewStep();
            } else {
                var content = question(next).getContent();
                questions.add(content);
                shownTexts.add(content.getQuestionText());
                content.getAnswerObjects().forEach(answer -> shownTexts.add(answer.getHyperText()));
                var judged = domain().judgeSupplementaryQuestion(main, stepContext(mistake, next.getNewStep()),
                        anyAnswer(content, singleChoice, matchingGroup), LANGUAGE);
                feedback = judged.getFeedback();
                newStep = judged.getNewStep();
            }
            shownTexts.add(feedback.getMessage().getMessage());
            feedbacks.add(feedback);
            if (newStep == null || feedback.getAction() == SupplementaryFeedbackDto.Action.Finish) {
                return new Discussion(questions, shownTexts, feedbacks, onAnswer);
            }
            step = stepData(mistake, newStep);
        }
        return fail("discussion did not finish in " + MAX_DISCUSSION_TURNS + " turns");
    }

    private static String withoutMarkup(String text) {
        return CODE.matcher(text).replaceAll("").replaceAll("<[^>]+>", "");
    }

    private static List<AnswerData> anyAnswer(QuestionContentData content,
                                              Function<List<AnswerObjectData>, AnswerObjectData> singleChoice,
                                              Function<List<AnswerObjectData>, AnswerObjectData> matchingGroup) {
        return switch (content.getQuestionType()) {
            case MATCHING -> {
                var group = matchingGroup.apply(groups(content.getAnswerObjects()));
                yield options(content.getAnswerObjects()).stream().<AnswerData>map(a -> new AnswerData.Pair(a, group)).toList();
            }
            case MULTI_CHOICE -> switched(options(content.getAnswerObjects()), options(content.getAnswerObjects()));
            default -> {
                var chosen = singleChoice.apply(content.getAnswerObjects());
                yield List.of(new AnswerData.Pair(chosen, chosen));
            }
        };
    }
}
