package org.vstu.compprehension.frontend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.authorization.TestUserService;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.survey.SurveyResultDto;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;

import java.util.Arrays;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class SurveyFrontendServiceTest extends AbstractIntegrationTest {

    private static final String HUMAN_OR_MACHINE_SURVEY = "IsCreatedByHuman";
    private static final long HUMAN_OR_MACHINE_QUESTION_ID = 1L;
    private static final String STRATEGY_FEEDBACK_SURVEY = "StrategyFeedbackSurvey";
    private static final long STRATEGY_FEEDBACK_QUESTION_ID = 3L;

    @Autowired private SurveyFrontendService service;
    @Autowired private ExerciseAttemptFrontendService attemptService;

    @AfterEach
    void resetCurrentUser() {
        TestUserService.reset();
    }

    // ---- опросы ----

    /** Опрос с одним вопросом. */
    @Test
    void getSurveyReturnsQuestionsAndOptions() {
        // Act.
        var survey = service.getSurvey(HUMAN_OR_MACHINE_SURVEY);

        // Assert.
        assertEquals(HUMAN_OR_MACHINE_SURVEY, survey.getSurveyId());
        assertEquals(15, survey.getOptions().getSize());
        assertEquals(1, survey.getQuestions().length);
        var question = survey.getQuestions()[0];
        assertEquals(HUMAN_OR_MACHINE_QUESTION_ID, question.getId());
        assertEquals("yes-no", question.getType());
        assertTrue(question.isRequired());
        assertTrue(question.getText().length() > 0);
    }

    /** Опрос со многими вопросами, в том числе необязательными. */
    @Test
    void getSurveyWithManyQuestions() {
        // Act.
        var survey = service.getSurvey(STRATEGY_FEEDBACK_SURVEY);

        // Assert.
        assertEquals(10, survey.getQuestions().length);
        assertTrue(Arrays.stream(survey.getQuestions()).anyMatch(q -> !q.isRequired()));
        assertTrue(Arrays.stream(survey.getQuestions()).anyMatch(q -> q.getId() == STRATEGY_FEEDBACK_QUESTION_ID));
    }

    /** Неизвестный опрос. */
    @Test
    void getUnknownSurveyFails() {
        // Act & Assert.
        assertThrows(NoSuchElementException.class, () -> service.getSurvey("NoSuchSurvey"));
    }

    // ---- ответы ----

    /** Голос за свой вопрос сохраняется. */
    @Test
    void saveAnswerStoresVoteForOwnQuestion() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = attemptService.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = attemptService.generateQuestion(attempt.getAttemptId());

        // Act.
        service.saveAnswer(TestData.Users.GLOBAL_STUDENT_ID, vote(HUMAN_OR_MACHINE_QUESTION_ID, question.getQuestionId(), "1"));

        // Assert.
        var votes = service.getUserAttemptVotes(TestData.Users.GLOBAL_STUDENT_ID, attempt.getAttemptId(), HUMAN_OR_MACHINE_SURVEY);
        assertEquals(1, votes.size());
        assertEquals(HUMAN_OR_MACHINE_QUESTION_ID, votes.getFirst().getSurveyQuestionId());
        assertEquals(question.getQuestionId(), votes.getFirst().getQuestionId());
        assertEquals("1", votes.getFirst().getAnswer());
    }

    /** Повторный голос заменяет прежний. */
    @Test
    void saveAnswerOverwritesPreviousVote() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = attemptService.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = attemptService.generateQuestion(attempt.getAttemptId());
        service.saveAnswer(TestData.Users.GLOBAL_STUDENT_ID, vote(HUMAN_OR_MACHINE_QUESTION_ID, question.getQuestionId(), "1"));

        // Act.
        service.saveAnswer(TestData.Users.GLOBAL_STUDENT_ID, vote(HUMAN_OR_MACHINE_QUESTION_ID, question.getQuestionId(), "0"));

        // Assert.
        var votes = service.getUserAttemptVotes(TestData.Users.GLOBAL_STUDENT_ID, attempt.getAttemptId(), HUMAN_OR_MACHINE_SURVEY);
        assertEquals(1, votes.size());
        assertEquals("0", votes.getFirst().getAnswer());
    }

    /** Голоса разделяются по опросам и попыткам. */
    @Test
    void getUserAttemptVotesFiltersBySurveyAndAttempt() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = attemptService.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = attemptService.generateQuestion(attempt.getAttemptId());
        service.saveAnswer(TestData.Users.GLOBAL_STUDENT_ID, vote(HUMAN_OR_MACHINE_QUESTION_ID, question.getQuestionId(), "1"));
        service.saveAnswer(TestData.Users.GLOBAL_STUDENT_ID, vote(STRATEGY_FEEDBACK_QUESTION_ID, question.getQuestionId(), "3"));

        // Act.
        var humanOrMachine = service.getUserAttemptVotes(TestData.Users.GLOBAL_STUDENT_ID, attempt.getAttemptId(), HUMAN_OR_MACHINE_SURVEY);
        var strategyFeedback = service.getUserAttemptVotes(TestData.Users.GLOBAL_STUDENT_ID, attempt.getAttemptId(), STRATEGY_FEEDBACK_SURVEY);
        var otherAttempt = service.getUserAttemptVotes(TestData.Users.GLOBAL_STUDENT_ID, Long.MIN_VALUE, HUMAN_OR_MACHINE_SURVEY);

        // Assert.
        assertEquals(1, humanOrMachine.size());
        assertEquals("1", humanOrMachine.getFirst().getAnswer());
        assertEquals(1, strategyFeedback.size());
        assertEquals("3", strategyFeedback.getFirst().getAnswer());
        assertTrue(otherAttempt.isEmpty());
    }

    /** Чужой вопрос голосовать нельзя. */
    @Test
    void saveAnswerForForeignQuestionFails() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = attemptService.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = attemptService.generateQuestion(attempt.getAttemptId());

        // Act & Assert.
        assertThrows(SecurityException.class, () -> service.saveAnswer(TestData.Users.MAIN_COURSE_STUDENT_ID,
                vote(HUMAN_OR_MACHINE_QUESTION_ID, question.getQuestionId(), "1")));
        assertTrue(service.getUserAttemptVotes(TestData.Users.GLOBAL_STUDENT_ID, attempt.getAttemptId(), HUMAN_OR_MACHINE_SURVEY).isEmpty());
    }

    /** Вопрос без попытки в опросах не участвует. */
    @Test
    void saveAnswerForQuestionWithoutAttemptFails() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var question = attemptService.generateQuestionByMetadata(TestData.ExpressionBank.MEMBER_ACCESS_PLUS.metadataId(), Language.ENGLISH);

        // Act & Assert.
        assertThrows(NoSuchElementException.class, () -> service.saveAnswer(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID,
                vote(HUMAN_OR_MACHINE_QUESTION_ID, question.getQuestionId(), "1")));
    }

    /** Несуществующий вопрос опроса. */
    @Test
    void saveAnswerForUnknownSurveyQuestionFails() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = attemptService.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = attemptService.generateQuestion(attempt.getAttemptId());

        // Act & Assert.
        assertThrows(NoSuchElementException.class, () -> service.saveAnswer(TestData.Users.GLOBAL_STUDENT_ID,
                vote(Long.MIN_VALUE, question.getQuestionId(), "1")));
    }

    private static SurveyResultDto vote(long surveyQuestionId, long questionId, String answer) {
        return new SurveyResultDto(surveyQuestionId, questionId, answer);
    }
}
