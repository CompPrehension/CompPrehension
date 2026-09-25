package org.vstu.compprehension.frontend;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.authorization.TestUserService;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.NewInteractionAnswerData;
import org.vstu.compprehension.data.question.NewInteractionData;
import org.vstu.compprehension.data.question.QuestionContentData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.SubmittedAnswerData;
import org.vstu.compprehension.data.questionoptions.MultiChoiceOptionsData;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.enums.QuestionType;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.services.QuestionDataService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
class MultiChoiceAnswerTest extends AbstractIntegrationTest {

    private static final int OPTION_ID = 0;

    @Autowired private QuestionDataService questionService;
    @Autowired private ExerciseAttemptFrontendService service;
    @PersistenceContext private EntityManager entityManager;

    @AfterEach
    void resetCurrentUser() {
        TestUserService.reset();
    }

    /** «Да» у единственного варианта становится значением, а не ссылкой на объект ответа. */
    @Test
    void singleOptionAnsweredYesResolvesToValue() {
        // Arrange.
        var question = singleOptionQuestion();

        // Act.
        var answers = questionService.resolveAnswers(question.getId(),
                List.of(new SubmittedAnswerData.Choice(OPTION_ID, MultiChoiceOptionsData.SWITCH_ON, null)));

        // Assert.
        assertEquals(1, answers.size());
        var choice = assertInstanceOf(AnswerData.Choice.class, answers.get(0));
        assertEquals(OPTION_ID, choice.left().getAnswerId());
        assertEquals(MultiChoiceOptionsData.SWITCH_ON, choice.value());
    }

    /** Пара объектов ответа для вопроса с множественным выбором отклоняется. */
    @Test
    void pairAnswerToMultiChoiceQuestionIsRejected() {
        // Arrange.
        var question = singleOptionQuestion();
        var pair = new SubmittedAnswerData.Pair(OPTION_ID, OPTION_ID, null);

        // Act.
        var error = assertThrows(InvalidDataAccessApiUsageException.class,
                () -> questionService.resolveAnswers(question.getId(), List.of(pair)));

        // Assert.
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
    }

    /** Сохранённый ответ возвращается в историю как [id, значение]. */
    @Test
    void recordedAnswerRoundTripsAsIdAndValue() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var question = singleOptionQuestion();
        questionService.recordInteraction(new NewInteractionData(question.getId(), InteractionType.SEND_RESPONSE,
                List.of(new NewInteractionAnswerData(null,
                        new SubmittedAnswerData.Choice(OPTION_ID, MultiChoiceOptionsData.SWITCH_ON, null))),
                List.of(), List.of(), 0));
        entityManager.flush();
        entityManager.clear();

        // Act.
        var reloaded = service.getQuestion(question.getId());

        // Assert.
        assertEquals(1, reloaded.getResponses().length);
        assertArrayEquals(new Long[] { (long) OPTION_ID, (long) MultiChoiceOptionsData.SWITCH_ON },
                reloaded.getResponses()[0].getAnswer());
    }

    private QuestionData singleOptionQuestion() {
        var option = AnswerObjectData.builder()
                .answerId(OPTION_ID)
                .hyperText("the only option")
                .domainInfo("")
                .build();
        return questionService.saveQuestion(QuestionData.of(QuestionContentData.builder()
                .domainId(TestData.Exercises.DOMAIN_ID)
                .questionType(QuestionType.MULTI_CHOICE)
                .questionText("Pick it")
                .questionDomainType("MultiChoiceTest")
                .options(new MultiChoiceOptionsData())
                .answerObjects(List.of(option))
                .build()), null, null);
    }
}
