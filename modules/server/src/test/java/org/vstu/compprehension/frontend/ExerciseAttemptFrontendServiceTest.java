package org.vstu.compprehension.frontend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.authorization.TestUserService;
import org.vstu.compprehension.enums.AttemptStatus;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.AnswerDto;
import org.vstu.compprehension.frontend.dto.InteractionDto;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.frontend.dto.question.MatchingQuestionDto;
import org.vstu.compprehension.frontend.dto.question.QuestionDto;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.infrastructure.TestData.BankQuestion;

import java.util.Arrays;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class ExerciseAttemptFrontendServiceTest extends AbstractIntegrationTest {

    private static final String ORDER = "ORDER";
    private static final String MATCHING = "MATCHING";
    private static final float GRADE_DELTA = 0.0001f;

    @Autowired private ExerciseAttemptFrontendService service;

    @AfterEach
    void resetCurrentUser() {
        TestUserService.reset();
    }

    // ---- попытки ----

    /** Новая попытка вне курса. */
    @Test
    void createExerciseAttemptStartsIncompleteWithoutQuestions() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var attempt = service.createExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);

        // Assert.
        assertNotNull(attempt.getAttemptId());
        assertEquals(TestData.GLOBAL_STUDENT_ID, attempt.getUserId());
        assertEquals(TestData.EXPRESSION_DT_EXERCISE_ID, attempt.getExerciseId());
        assertNull(attempt.getCourseId());
        assertEquals(AttemptStatus.INCOMPLETE, attempt.getStatus());
        assertEquals(0, attempt.getQuestionIds().length);
    }

    /** Новая попытка внутри курса. */
    @Test
    void createExerciseAttemptInsideCourseKeepsCourse() {
        // Arrange.
        TestUserService.actAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var attempt = service.createExerciseAttempt(TestData.MAIN_COURSE_EXERCISE_ID, TestData.MAIN_COURSE_STUDENT_ID, TestData.MAIN_COURSE_ID);

        // Assert.
        assertEquals(TestData.MAIN_COURSE_ID, attempt.getCourseId());
        assertEquals(TestData.MAIN_COURSE_EXERCISE_ID, attempt.getExerciseId());
    }

    /** Чтение попытки по id. */
    @Test
    void getExerciseAttemptReturnsCreatedAttempt() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);
        var created = service.createExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);

        // Act.
        var loaded = service.getExerciseAttempt(created.getAttemptId());

        // Assert.
        assertNotNull(loaded);
        assertEquals(created.getAttemptId(), loaded.getAttemptId());
        assertEquals(created.getUserId(), loaded.getUserId());
        assertEquals(created.getExerciseId(), loaded.getExerciseId());
        assertEquals(AttemptStatus.INCOMPLETE, loaded.getStatus());
    }

    /** Несуществующая попытка. */
    @Test
    void getExerciseAttemptReturnsNullForUnknownId() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var loaded = service.getExerciseAttempt(Long.MIN_VALUE);

        // Assert.
        assertNull(loaded);
    }

    /** Незавершённой попытки ещё нет. */
    @Test
    void getExistingExerciseAttemptReturnsNullWhenNothingStarted() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var existing = service.getExistingExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);

        // Assert.
        assertNull(existing);
    }

    /** Поиск своей незавершённой попытки. */
    @Test
    void getExistingExerciseAttemptFindsIncompleteAttempt() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);
        var created = service.createExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);

        // Act.
        var existing = service.getExistingExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);

        // Assert.
        assertNotNull(existing);
        assertEquals(created.getAttemptId(), existing.getAttemptId());
    }

    /** Чужая попытка не находится. */
    @Test
    void getExistingExerciseAttemptIgnoresOtherUsersAttempts() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);
        service.createExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);

        // Act.
        var existing = service.getExistingExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.USER_WITHOUT_ROLES_ID, null);

        // Assert.
        assertNull(existing);
    }

    /** Отладочная попытка: вопросы всех стадий созданы, но не решены. */
    @Test
    void createSolvedExerciseAttemptGeneratesAllStageQuestions() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var attempt = service.createSolvedExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);

        // Assert.
        assertEquals(AttemptStatus.INCOMPLETE, attempt.getStatus());
        assertEquals(TestData.EXPRESSION_DT_EXERCISE_QUESTIONS, attempt.getQuestionIds().length);
        assertEquals(TestData.EXPRESSION_DT_EXERCISE_QUESTIONS,
                Arrays.stream(attempt.getQuestionIds()).map(id -> service.getQuestion(id).getQuestionMetadataId()).distinct().count());
    }

    /** Владельцу открыты и попытка, и её вопрос. */
    @Test
    void ensureCanAccessAllowsAttemptOwner() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);
        var question = service.generateQuestion(attempt.getAttemptId());

        // Act & Assert.
        assertDoesNotThrow(() -> service.ensureCanAccessAttempt(TestData.GLOBAL_STUDENT_ID, attempt.getAttemptId()));
        assertDoesNotThrow(() -> service.ensureCanAccessQuestion(TestData.GLOBAL_STUDENT_ID, question.getQuestionId()));
    }

    // ---- генерация вопросов ----

    /** Вопрос из банка привязывается к попытке. */
    @Test
    void generateQuestionTakesBankQuestionAndAttachesItToAttempt() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);

        // Act.
        var question = service.generateQuestion(attempt.getAttemptId());

        // Assert.
        var bankQuestion = TestData.bankQuestion(question.getQuestionMetadataId());
        assertEquals(ORDER, question.getType());
        assertEquals(bankQuestion.steps() + 1, question.getAnswers().length);
        assertFalse(question.getText().isBlank());
        assertArrayEquals(new Long[] { question.getQuestionId() }, service.getExerciseAttempt(attempt.getAttemptId()).getQuestionIds());
    }

    /** Второй вопрос попытки берётся из других метаданных. */
    @Test
    void generateQuestionDoesNotRepeatBankQuestionWithinAttempt() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);

        // Act.
        var first = service.generateQuestion(attempt.getAttemptId());
        var second = service.generateQuestion(attempt.getAttemptId());

        // Assert.
        assertNotEquals(first.getQuestionId(), second.getQuestionId());
        assertNotEquals(first.getQuestionMetadataId(), second.getQuestionMetadataId());
        assertArrayEquals(new Long[] { first.getQuestionId(), second.getQuestionId() },
                service.getExerciseAttempt(attempt.getAttemptId()).getQuestionIds());
    }

    /** Вопрос по метаданным, без попытки. */
    @Test
    void generateQuestionByMetadataBuildsQuestionWithoutAttempt() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var question = service.generateQuestionByMetadata(TestData.MEMBER_ACCESS_PLUS.metadataId(), Language.ENGLISH);

        // Assert.
        assertNotNull(question.getQuestionId());
        assertEquals(TestData.MEMBER_ACCESS_PLUS.metadataId(), question.getQuestionMetadataId());
        assertEquals(ORDER, question.getType());
        assertArrayEquals(new String[] { "->", "+", "student_end_evaluation" },
                Arrays.stream(question.getAnswers()).map(a -> a.getText()).toArray(String[]::new));
        assertTrue(question.getText().contains("sb_w"));
    }

    /** Несуществующие метаданные. */
    @Test
    void generateQuestionByMetadataFailsForUnknownMetadata() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act & Assert.
        var error = assertThrows(RuntimeException.class,
                () -> service.generateQuestionByMetadata(Integer.MIN_VALUE, Language.ENGLISH));
        assertTrue(error.getMessage().contains("not found"));
    }

    /** Повторное чтение созданного вопроса. */
    @Test
    void getQuestionReturnsGeneratedQuestion() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);
        var generated = service.generateQuestionByMetadata(TestData.MUL_PLUS_MINUS.metadataId(), Language.ENGLISH);

        // Act.
        var loaded = service.getQuestion(generated.getQuestionId());

        // Assert.
        assertEquals(generated.getQuestionId(), loaded.getQuestionId());
        assertEquals(generated.getQuestionMetadataId(), loaded.getQuestionMetadataId());
        assertEquals(generated.getType(), loaded.getType());
        assertEquals(generated.getText(), loaded.getText());
        assertEquals(generated.getAnswers().length, loaded.getAnswers().length);
        assertEquals(0, loaded.getResponses().length);
    }

    // ---- ответы на вопрос без попытки ----

    /** Верный первый оператор. */
    @Test
    void addQuestionAnswerAcceptsCorrectFirstStepWithoutAttempt() {
        // Arrange.
        var bankQuestion = TestData.ASSIGN_UNARY_MINUS_PLUS;
        var question = attemptlessQuestion(bankQuestion);

        // Act.
        var feedback = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(0)));

        // Assert.
        assertTrue(feedback.isCorrect());
        assertEquals(bankQuestion.steps() - 1, feedback.getStepsLeft());
        assertEquals(1, feedback.getCorrectSteps());
        assertEquals(0, feedback.getStepsWithErrors());
        assertEquals(1f, feedback.getGrade(), GRADE_DELTA);
        assertEquals(Decision.CONTINUE, feedback.getStrategyDecision());
        assertAnswerIds(feedback, bankQuestion.operatorAt(0));
        assertSingleMessage(feedback, FeedbackDto.MessageType.SUCCESS);
    }

    /** Оператор вне очереди: ошибка приоритета. */
    @Test
    void addQuestionAnswerRejectsOperatorOutOfOrder() {
        // Arrange.
        var bankQuestion = TestData.PARENTHESES_AND_UNARY_MINUS;
        var question = attemptlessQuestion(bankQuestion);

        // Act.
        var feedback = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(1)));

        // Assert.
        assertFalse(feedback.isCorrect());
        assertEquals(bankQuestion.steps(), feedback.getStepsLeft());
        assertEquals(0, feedback.getCorrectSteps());
        assertEquals(1, feedback.getStepsWithErrors());
        assertEquals(0, feedback.getCorrectAnswers().length);
        assertSingleMessage(feedback, FeedbackDto.MessageType.ERROR);
        assertTrue(feedback.getMessages()[0].getViolationLaws().stream()
                .anyMatch(law -> law.getName().equals("left_competing_to_right_precedence") && law.isCanCreateSupplementaryQuestion()));
    }

    /** Преждевременное «всё вычислено». */
    @Test
    void addQuestionAnswerRejectsEarlyFinish() {
        // Arrange.
        var bankQuestion = TestData.MEMBER_ACCESS_PLUS;
        var question = attemptlessQuestion(bankQuestion);

        // Act.
        var feedback = service.addQuestionAnswer(interaction(question, bankQuestion.endEvaluationAnswerId()));

        // Assert.
        assertFalse(feedback.isCorrect());
        assertEquals(bankQuestion.steps(), feedback.getStepsLeft());
        assertEquals(1, feedback.getStepsWithErrors());
        assertSingleMessage(feedback, FeedbackDto.MessageType.ERROR);
        assertFalse(feedback.getMessages()[0].getViolationLaws().isEmpty());
    }

    /** Полное решение по шагам. */
    @Test
    void addQuestionAnswerCompletesQuestionStepByStep() {
        // Arrange.
        var bankQuestion = TestData.MUL_PLUS_MINUS;
        var question = attemptlessQuestion(bankQuestion);

        // Act.
        var feedback = solveByAnswers(question, bankQuestion);

        // Assert.
        assertTrue(feedback.isCorrect());
        assertEquals(0, feedback.getStepsLeft());
        assertEquals(bankQuestion.steps(), feedback.getCorrectSteps());
        assertEquals(0, feedback.getStepsWithErrors());
        assertAnswerIds(feedback, bankQuestion.evaluationOrder());
        assertEquals(bankQuestion.steps(), service.getQuestion(question.getQuestionId()).getResponses().length);
    }

    /** Ошибка после верного шага не отменяет его. */
    @Test
    void addQuestionAnswerKeepsEarlierCorrectStepsAfterMistake() {
        // Arrange.
        var bankQuestion = TestData.ASSIGN_UNARY_MINUS_PLUS;
        var question = attemptlessQuestion(bankQuestion);
        var afterFirst = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(0)));

        // Act.
        var feedback = service.addQuestionAnswer(
                interaction(question, afterFirst.getCorrectAnswers(), bankQuestion.operatorAt(2)));

        // Assert.
        assertFalse(feedback.isCorrect());
        assertEquals(1, feedback.getCorrectSteps());
        assertEquals(1, feedback.getStepsWithErrors());
        assertEquals(bankQuestion.steps() - 1, feedback.getStepsLeft());
        assertAnswerIds(feedback, bankQuestion.operatorAt(0));
    }

    // ---- подсказки ----

    /** Подсказка для вопроса без попытки. */
    @Test
    void generateNextCorrectAnswerWorksForQuestionWithoutAttempt() {
        // Arrange.
        var bankQuestion = TestData.MEMBER_ACCESS_PLUS;
        var question = attemptlessQuestion(bankQuestion);

        // Act.
        var feedback = service.generateNextCorrectAnswer(question.getQuestionId());

        // Assert.
        assertTrue(feedback.isCorrect());
        assertEquals(bankQuestion.steps() - 1, feedback.getStepsLeft());
        assertEquals(1, feedback.getCorrectSteps());
        assertEquals(0, feedback.getStepsWithErrors());
        assertEquals(1f, feedback.getGrade(), GRADE_DELTA);
        assertEquals(Decision.CONTINUE, feedback.getStrategyDecision());
        assertAnswerIds(feedback, bankQuestion.operatorAt(0));
        assertSingleMessage(feedback, FeedbackDto.MessageType.SUCCESS);
    }

    /** Подсказка достраивает уже данные ответы. */
    @Test
    void generateNextCorrectAnswerCarriesOverAnswersAlreadyGiven() {
        // Arrange.
        var bankQuestion = TestData.ASSIGN_UNARY_MINUS_PLUS;
        var question = attemptlessQuestion(bankQuestion);
        service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(0)));

        // Act.
        var feedback = service.generateNextCorrectAnswer(question.getQuestionId());

        // Assert.
        assertTrue(feedback.isCorrect());
        assertEquals(bankQuestion.steps() - 2, feedback.getStepsLeft());
        assertEquals(2, feedback.getCorrectSteps());
        assertAnswerIds(feedback, bankQuestion.operatorAt(0), bankQuestion.operatorAt(1));
    }

    /** Решение подсказками до конца. */
    @Test
    void generateNextCorrectAnswerSolvesQuestionToTheEnd() {
        // Arrange.
        var bankQuestion = TestData.MUL_PLUS_MINUS;
        var question = attemptlessQuestion(bankQuestion);

        // Act.
        var feedback = solveByHints(question);

        // Assert.
        assertTrue(feedback.isCorrect());
        assertEquals(0, feedback.getStepsLeft());
        assertEquals(bankQuestion.steps(), feedback.getCorrectSteps());
        assertAnswerIds(feedback, bankQuestion.evaluationOrder());
        var reloaded = service.getQuestion(question.getQuestionId());
        assertEquals(bankQuestion.steps(), reloaded.getResponses().length);
        assertTrue(reloaded.getFeedback().isCorrect());
        assertEquals(0, reloaded.getFeedback().getStepsLeft());
    }

    /** Подсказка после ошибки начинает с верного шага. */
    @Test
    void generateNextCorrectAnswerIgnoresPreviousMistake() {
        // Arrange.
        var bankQuestion = TestData.PARENTHESES_AND_UNARY_MINUS;
        var question = attemptlessQuestion(bankQuestion);
        service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(1)));

        // Act.
        var feedback = service.generateNextCorrectAnswer(question.getQuestionId());

        // Assert.
        assertTrue(feedback.isCorrect());
        assertEquals(1, feedback.getCorrectSteps());
        assertEquals(1, feedback.getStepsWithErrors());
        assertEquals(bankQuestion.steps() - 1, feedback.getStepsLeft());
        assertAnswerIds(feedback, bankQuestion.operatorAt(0));
    }

    // ---- оценка стратегией внутри попытки ----

    /** Верный ответ в попытке оценивается стратегией. */
    @Test
    void addQuestionAnswerInsideAttemptIsGradedByStrategy() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);
        var question = service.generateQuestion(attempt.getAttemptId());
        var bankQuestion = TestData.bankQuestion(question.getQuestionMetadataId());

        // Act.
        var feedback = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(0)));

        // Assert.
        assertTrue(feedback.isCorrect());
        assertEquals(1f / 4 / TestData.EXPRESSION_DT_EXERCISE_QUESTIONS, feedback.getGrade(), GRADE_DELTA);
        assertEquals(Decision.CONTINUE, feedback.getStrategyDecision());
        assertEquals(AttemptStatus.INCOMPLETE, service.getExerciseAttempt(attempt.getAttemptId()).getStatus());
    }

    /** Ошибка в попытке: нулевая оценка. */
    @Test
    void wrongAnswerInsideAttemptGetsZeroGrade() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);
        var question = service.generateQuestion(attempt.getAttemptId());
        var bankQuestion = TestData.bankQuestion(question.getQuestionMetadataId());

        // Act.
        var feedback = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(1)));

        // Assert.
        assertFalse(feedback.isCorrect());
        assertEquals(0f, feedback.getGrade(), GRADE_DELTA);
        assertEquals(Decision.CONTINUE, feedback.getStrategyDecision());
    }

    /** Подсказки не дают баллов. */
    @Test
    void hintsInsideAttemptDoNotRaiseGrade() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);
        var question = service.generateQuestion(attempt.getAttemptId());

        // Act.
        var feedback = solveByHints(question);

        // Assert.
        assertEquals(0, feedback.getStepsLeft());
        assertEquals(0f, feedback.getGrade(), GRADE_DELTA);
    }

    /** Решён один вопрос из двух: попытка продолжается. */
    @Test
    void attemptStaysIncompleteWhileStageHasUnansweredQuestions() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);
        var question = service.generateQuestion(attempt.getAttemptId());

        // Act.
        var feedback = solveByAnswers(question, TestData.bankQuestion(question.getQuestionMetadataId()));

        // Assert.
        assertEquals(0, feedback.getStepsLeft());
        assertEquals(Decision.CONTINUE, feedback.getStrategyDecision());
        var inProgress = service.getExerciseAttempt(attempt.getAttemptId());
        assertEquals(AttemptStatus.INCOMPLETE, inProgress.getStatus());
        assertTrue(inProgress.getQuestionIds().length < TestData.EXPRESSION_DT_EXERCISE_QUESTIONS);
        assertNotNull(service.getExistingExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null));
    }

    /** Решены все вопросы стадии: попытка завершена. */
    @Test
    void completingEveryStageQuestionFinishesAttempt() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);
        var first = service.generateQuestion(attempt.getAttemptId());
        var firstBankQuestion = TestData.bankQuestion(first.getQuestionMetadataId());
        solveByAnswers(first, firstBankQuestion);
        var second = service.generateQuestion(attempt.getAttemptId());
        var secondBankQuestion = TestData.bankQuestion(second.getQuestionMetadataId());

        // Act.
        var feedback = solveByAnswers(second, secondBankQuestion);

        // Assert.
        assertEquals(Decision.FINISH, feedback.getStrategyDecision());
        assertEquals((firstBankQuestion.steps() + secondBankQuestion.steps()) / 4f / TestData.EXPRESSION_DT_EXERCISE_QUESTIONS,
                feedback.getGrade(), GRADE_DELTA);
        var finished = service.getExerciseAttempt(attempt.getAttemptId());
        assertEquals(AttemptStatus.COMPLETED_BY_USER, finished.getStatus());
        assertArrayEquals(new Long[] { first.getQuestionId(), second.getQuestionId() }, finished.getQuestionIds());
        assertNull(service.getExistingExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null));
    }

    // ---- дополнительные вопросы ----

    /** Доп. вопрос после ошибки приоритета. */
    @Test
    void generateSupplementaryQuestionAfterPrecedenceMistake() {
        // Arrange.
        var bankQuestion = TestData.PARENTHESES_AND_UNARY_MINUS;
        var question = attemptlessQuestion(bankQuestion);
        var mistake = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(1)));

        // Act.
        var supplementary = service.generateSupplementaryQuestion(question.getQuestionId(), violationLawsOf(mistake));

        // Assert.
        assertNull(supplementary.getMessage());
        var supplementaryQuestion = assertInstanceOf(MatchingQuestionDto.class, supplementary.getQuestion());
        assertEquals(MATCHING, supplementaryQuestion.getType());
        assertNotEquals(question.getQuestionId(), supplementaryQuestion.getQuestionId());
        assertFalse(supplementaryQuestion.getText().isBlank());
        assertTrue(supplementaryQuestion.getAnswers().length > 0);
        assertTrue(supplementaryQuestion.getGroups().length > 0);
    }

    /** Неверное сопоставление в доп. вопросе. */
    @Test
    void addSupplementaryQuestionAnswerExplainsWrongMatching() {
        // Arrange.
        var bankQuestion = TestData.PARENTHESES_AND_UNARY_MINUS;
        var question = attemptlessQuestion(bankQuestion);
        var mistake = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(1)));
        var supplementary = (MatchingQuestionDto) service
                .generateSupplementaryQuestion(question.getQuestionId(), violationLawsOf(mistake)).getQuestion();

        // Act.
        var feedback = service.addSupplementaryQuestionAnswer(everythingToFirstGroup(supplementary));

        // Assert.
        assertEquals(FeedbackDto.MessageType.ERROR, feedback.getMessage().getType());
        assertEquals(SupplementaryFeedbackDto.Action.ContinueManual, feedback.getAction());
        assertFalse(feedback.getMessage().getMessage().isBlank());
    }

    /** Следующий шаг цепочки доп. вопросов. */
    @Test
    void supplementaryChainContinuesAfterAnsweredStep() {
        // Arrange.
        var bankQuestion = TestData.PARENTHESES_AND_UNARY_MINUS;
        var question = attemptlessQuestion(bankQuestion);
        var mistake = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(1)));
        var laws = violationLawsOf(mistake);
        var supplementary = (MatchingQuestionDto) service.generateSupplementaryQuestion(question.getQuestionId(), laws).getQuestion();
        service.addSupplementaryQuestionAnswer(everythingToFirstGroup(supplementary));

        // Act.
        var next = service.generateSupplementaryQuestion(question.getQuestionId(), laws);

        // Assert.
        assertNull(next.getQuestion());
        assertNotNull(next.getMessage());
        assertEquals(FeedbackDto.MessageType.SUCCESS, next.getMessage().getMessage().getType());
        assertEquals(SupplementaryFeedbackDto.Action.ContinueAuto, next.getMessage().getAction());
    }

    /** Доп. вопрос не попадает в список вопросов попытки. */
    @Test
    void supplementaryQuestionInsideAttemptDoesNotBecomeAttemptQuestion() {
        // Arrange.
        TestUserService.actAs(TestData.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.EXPRESSION_DT_EXERCISE_ID, TestData.GLOBAL_STUDENT_ID, null);
        var question = service.generateQuestion(attempt.getAttemptId());
        var bankQuestion = TestData.bankQuestion(question.getQuestionMetadataId());
        var mistake = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(1)));

        // Act.
        var supplementary = service.generateSupplementaryQuestion(question.getQuestionId(), violationLawsOf(mistake));

        // Assert.
        assertNotNull(supplementary.getQuestion());
        assertArrayEquals(new Long[] { question.getQuestionId() }, service.getExerciseAttempt(attempt.getAttemptId()).getQuestionIds());
    }

    // ---- вспомогательное ----

    private QuestionDto attemptlessQuestion(BankQuestion bankQuestion) {
        TestUserService.actAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);
        return service.generateQuestionByMetadata(bankQuestion.metadataId(), Language.ENGLISH);
    }

    private static InteractionDto interaction(QuestionDto question, long... answerIds) {
        return interaction(question, new AnswerDto[0], answerIds);
    }

    private static InteractionDto interaction(QuestionDto question, AnswerDto[] alreadyGiven, long... answerIds) {
        var fresh = LongStream.of(answerIds).mapToObj(id -> new AnswerDto(id, id, true, null));
        return new InteractionDto(question.getQuestionId(),
                Stream.concat(Arrays.stream(alreadyGiven), fresh).toArray(AnswerDto[]::new));
    }

    private FeedbackDto solveByAnswers(QuestionDto question, BankQuestion bankQuestion) {
        FeedbackDto feedback = null;
        var given = new AnswerDto[0];
        for (int step = 0; step < bankQuestion.steps(); step++) {
            feedback = service.addQuestionAnswer(interaction(question, given, bankQuestion.operatorAt(step)));
            assertTrue(feedback.isCorrect(), "шаг " + step + " для " + bankQuestion.expression());
            given = feedback.getCorrectAnswers();
        }
        return feedback;
    }

    private FeedbackDto solveByHints(QuestionDto question) {
        var feedback = service.generateNextCorrectAnswer(question.getQuestionId());
        while (feedback.getStepsLeft() > 0) {
            feedback = service.generateNextCorrectAnswer(question.getQuestionId());
        }
        return feedback;
    }

    private static String[] violationLawsOf(FeedbackDto mistake) {
        return Arrays.stream(mistake.getMessages())
                .flatMap(message -> message.getViolationLaws().stream())
                .filter(law -> law.isCanCreateSupplementaryQuestion())
                .map(law -> law.getName())
                .toArray(String[]::new);
    }

    private static InteractionDto everythingToFirstGroup(MatchingQuestionDto question) {
        var group = question.getGroups()[0].getId();
        return new InteractionDto(question.getQuestionId(), Arrays.stream(question.getAnswers())
                .map(answer -> new AnswerDto(answer.getId(), group, true, null))
                .toArray(AnswerDto[]::new));
    }

    private static void assertAnswerIds(FeedbackDto feedback, long... expected) {
        assertArrayEquals(expected, Arrays.stream(feedback.getCorrectAnswers())
                .mapToLong(answer -> answer.getAnswer()[0])
                .toArray());
        assertTrue(Arrays.stream(feedback.getCorrectAnswers())
                .allMatch(answer -> answer.getAnswer()[0].equals(answer.getAnswer()[1])));
    }

    private static void assertSingleMessage(FeedbackDto feedback, FeedbackDto.MessageType type) {
        assertEquals(1, feedback.getMessages().length);
        assertEquals(type, feedback.getMessages()[0].getType());
        assertFalse(feedback.getMessages()[0].getMessage().isBlank());
    }
}
