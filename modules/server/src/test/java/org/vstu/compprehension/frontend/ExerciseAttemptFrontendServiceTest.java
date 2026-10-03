package org.vstu.compprehension.frontend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.authorization.TestUserService;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionInteractionData;
import org.vstu.compprehension.services.ExerciseAttemptDataService;
import org.vstu.compprehension.services.QuestionDataService;
import org.vstu.compprehension.data.questionoptions.MultiChoiceOptionsData;
import org.vstu.compprehension.entities.ExerciseEntity;
import org.vstu.compprehension.enums.AttemptStatus;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.AnswerDto;
import org.vstu.compprehension.frontend.dto.InteractionDto;
import org.vstu.compprehension.frontend.dto.SupplementaryQuestionDto;
import org.vstu.compprehension.frontend.dto.feedback.ClarificationAnswerDto;
import org.vstu.compprehension.frontend.dto.feedback.ClarificationDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.frontend.dto.question.MatchingQuestionDto;
import org.vstu.compprehension.frontend.dto.question.QuestionDto;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData.ExpressionBank.BankQuestion;
import org.vstu.compprehension.infrastructure.TestData;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private static final String MULTI_CHOICE = "MULTI_CHOICE";
    private static final float GRADE_DELTA = 0.0001f;

    @Autowired private ExerciseAttemptFrontendService service;
    @Autowired private AuthFrontendService authService;
    @Autowired private AbstractStrategyFactory strategyFactory;
    @Autowired private ExerciseAttemptDataService attemptDataService;
    @Autowired private QuestionDataService questionDataService;
    @PersistenceContext private EntityManager entityManager;

    @AfterEach
    void resetCurrentUser() {
        TestUserService.reset();
    }

    // ---- попытки ----

    /** Новая попытка вне курса. */
    @Test
    void createExerciseAttemptStartsIncompleteWithoutQuestions() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var attempt = service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);

        // Assert.
        assertNotNull(attempt.getAttemptId());
        assertEquals(TestData.Users.GLOBAL_STUDENT_ID, attempt.getUserId());
        assertEquals(TestData.Exercises.EXPRESSION_DT_ID, attempt.getExerciseId());
        assertNull(attempt.getCourseId());
        assertEquals(AttemptStatus.INCOMPLETE, attempt.getStatus());
        assertEquals(0, attempt.getQuestionIds().length);
    }

    /** Новая попытка внутри курса. */
    @Test
    void createExerciseAttemptInsideCourseKeepsCourse() {
        // Arrange.
        TestUserService.actAs(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Act.
        var attempt = service.createExerciseAttempt(TestData.Exercises.MAIN_COURSE_ID, TestData.Users.MAIN_COURSE_STUDENT_ID, TestData.Courses.MAIN_ID);

        // Assert.
        assertEquals(TestData.Courses.MAIN_ID, attempt.getCourseId());
        assertEquals(TestData.Exercises.MAIN_COURSE_ID, attempt.getExerciseId());
    }

    /** Чтение попытки по id. */
    @Test
    void getExerciseAttemptReturnsCreatedAttempt() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var created = service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);

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
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var loaded = service.getExerciseAttempt(Long.MIN_VALUE);

        // Assert.
        assertNull(loaded);
    }

    /** Незавершённой попытки ещё нет. */
    @Test
    void getExistingExerciseAttemptReturnsNullWhenNothingStarted() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var existing = service.getExistingExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);

        // Assert.
        assertNull(existing);
    }

    /** Поиск своей незавершённой попытки. */
    @Test
    void getExistingExerciseAttemptFindsIncompleteAttempt() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var created = service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);

        // Act.
        var existing = service.getExistingExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);

        // Assert.
        assertNotNull(existing);
        assertEquals(created.getAttemptId(), existing.getAttemptId());
    }

    /** Чужая попытка не находится. */
    @Test
    void getExistingExerciseAttemptIgnoresOtherUsersAttempts() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);

        // Act.
        var existing = service.getExistingExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.WITHOUT_ROLES_ID, null);

        // Assert.
        assertNull(existing);
    }

    /** Отладочная попытка: вопросы всех стадий созданы, но не решены. */
    @Test
    void createSolvedExerciseAttemptGeneratesAllStageQuestions() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var attempt = service.createSolvedExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);

        // Assert.
        assertEquals(AttemptStatus.INCOMPLETE, attempt.getStatus());
        assertEquals(TestData.Exercises.EXPRESSION_DT_QUESTIONS, attempt.getQuestionIds().length);
        assertEquals(TestData.Exercises.EXPRESSION_DT_QUESTIONS,
                Arrays.stream(attempt.getQuestionIds()).map(id -> service.getQuestion(id).getQuestionMetadataId()).distinct().count());
    }

    /** Владельцу открыты и попытка, и её вопрос. */
    @Test
    void ensureCanAccessAllowsAttemptOwner() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = service.generateQuestion(attempt.getAttemptId());

        // Act & Assert.
        assertDoesNotThrow(() -> authService.ensureCanReadAttempt(TestData.Users.GLOBAL_STUDENT_ID, attempt.getAttemptId()));
        assertDoesNotThrow(() -> authService.ensureCanWriteAttempt(TestData.Users.GLOBAL_STUDENT_ID, attempt.getAttemptId()));
        assertDoesNotThrow(() -> authService.ensureCanReadQuestion(TestData.Users.GLOBAL_STUDENT_ID, question.getQuestionId()));
        assertDoesNotThrow(() -> authService.ensureCanWriteQuestion(TestData.Users.GLOBAL_STUDENT_ID, question.getQuestionId()));
    }

    // ---- генерация вопросов ----

    /** Вопрос из банка привязывается к попытке. */
    @Test
    void generateQuestionTakesBankQuestionAndAttachesItToAttempt() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);

        // Act.
        var question = service.generateQuestion(attempt.getAttemptId());

        // Assert.
        var bankQuestion = TestData.ExpressionBank.byMetadataId(question.getQuestionMetadataId());
        assertEquals(ORDER, question.getType());
        assertEquals(bankQuestion.steps() + 1, question.getAnswers().length);
        assertFalse(question.getText().isBlank());
        assertArrayEquals(new Long[] { question.getQuestionId() }, service.getExerciseAttempt(attempt.getAttemptId()).getQuestionIds());
    }

    /** Второй вопрос попытки берётся из других метаданных. */
    @Test
    void generateQuestionDoesNotRepeatBankQuestionWithinAttempt() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);

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
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var question = service.generateQuestionByMetadata(TestData.ExpressionBank.MEMBER_ACCESS_PLUS.metadataId(), Language.ENGLISH);

        // Assert.
        assertNotNull(question.getQuestionId());
        assertEquals(TestData.ExpressionBank.MEMBER_ACCESS_PLUS.metadataId(), question.getQuestionMetadataId());
        assertEquals(ORDER, question.getType());
        assertArrayEquals(new String[] { "->", "+", "student_end_evaluation" },
                Arrays.stream(question.getAnswers()).map(a -> a.getText()).toArray(String[]::new));
        assertTrue(question.getText().contains("sb_w"));
    }

    /** Несуществующие метаданные. */
    @Test
    void generateQuestionByMetadataFailsForUnknownMetadata() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act & Assert.
        var error = assertThrows(RuntimeException.class,
                () -> service.generateQuestionByMetadata(Integer.MIN_VALUE, Language.ENGLISH));
        assertTrue(error.getMessage().contains("not found"));
    }

    /** Повторное чтение созданного вопроса. */
    @Test
    void getQuestionReturnsGeneratedQuestion() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var generated = service.generateQuestionByMetadata(TestData.ExpressionBank.MUL_PLUS_MINUS.metadataId(), Language.ENGLISH);

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
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.ASSIGN_UNARY_MINUS_PLUS;
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
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.PARENTHESES_AND_UNARY_MINUS;
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
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.MEMBER_ACCESS_PLUS;
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
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.MUL_PLUS_MINUS;
        var question = attemptlessQuestion(bankQuestion);

        // Act.
        var feedback = solveByAnswers(question, bankQuestion);

        // Assert.
        assertTrue(feedback.isCorrect());
        assertEquals(0, feedback.getStepsLeft());
        assertEquals(bankQuestion.steps(), feedback.getCorrectSteps());
        assertEquals(0, feedback.getStepsWithErrors());
        assertAnswerIds(feedback, bankQuestion.evaluationOrder());
        assertNull(feedback.getMessages(), "о решении задачи сообщает интерфейс, у последнего ответа своего сообщения нет");
        assertEquals(bankQuestion.steps(), service.getQuestion(question.getQuestionId()).getResponses().length);
    }

    /** Ошибка после верного шага не отменяет его. */
    @Test
    void addQuestionAnswerKeepsEarlierCorrectStepsAfterMistake() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.ASSIGN_UNARY_MINUS_PLUS;
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

    /** Порядок ответов взаимодействия переживает перечитывание вопроса из базы. */
    @Test
    void addQuestionAnswerKeepsAnswerOrderWhenQuestionIsReloaded() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.ASSIGN_UNARY_MINUS_PLUS;
        var question = attemptlessQuestion(bankQuestion);
        var afterFirst = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(0)));
        var afterSecond = service.addQuestionAnswer(
                interaction(question, afterFirst.getCorrectAnswers(), bankQuestion.operatorAt(1)));
        resetPersistenceContext();

        // Act.
        var reloaded = service.getQuestion(question.getQuestionId());
        var mistake = service.addQuestionAnswer(
                interaction(question, reloaded.getResponses(), bankQuestion.endEvaluationAnswerId()));

        // Assert.
        assertAnswerIds(afterSecond, bankQuestion.operatorAt(0), bankQuestion.operatorAt(1));
        assertArrayEquals(new long[] {bankQuestion.operatorAt(0), bankQuestion.operatorAt(1)},
                Arrays.stream(reloaded.getResponses()).mapToLong(answer -> answer.getAnswer()[0]).toArray());
        assertFalse(mistake.isCorrect());
        assertAnswerIds(mistake, bankQuestion.operatorAt(0), bankQuestion.operatorAt(1));
    }

    // ---- подсказки ----

    /** Подсказка для вопроса без попытки. */
    @Test
    void generateNextCorrectAnswerWorksForQuestionWithoutAttempt() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.MEMBER_ACCESS_PLUS;
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
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.ASSIGN_UNARY_MINUS_PLUS;
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
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.MUL_PLUS_MINUS;
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
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.PARENTHESES_AND_UNARY_MINUS;
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

    // ---- гипотезы о рассуждении студента ----

    /** Ответ студента сохраняется вместе с гипотезой, которой объясняется его рассуждение. */
    @Test
    void addQuestionAnswerRecordsHypothesesOfStudentReasoning() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var question = service.generateQuestionByMetadata(
                TestData.TypeEvaluationBank.AVERAGE_OF_GRADES_METADATA_ID, Language.ENGLISH);
        var lengthAsList = new AnswerDto(TestData.TypeEvaluationBank.LEN_SLOT,
                TestData.TypeEvaluationBank.LIST_INT_TYPE, true, null);

        // Act.
        var feedback = service.addQuestionAnswer(
                new InteractionDto(question.getQuestionId(), new AnswerDto[] { lengthAsList }));

        // Assert.
        assertFalse(feedback.isCorrect());
        resetPersistenceContext();
        assertEquals(List.of("argument_type:false"), recordedHypotheses(question.getQuestionId()));
    }

    /** В домене потока управления на онтологии верный ответ без нарушений и записывается, и сообщается верным. */
    @Test
    void answerWithoutViolationsInOntologyControlFlowDomainIsCorrect() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var question = service.generateQuestionByMetadata(TestData.ControlFlowBank.FORMAT_FREE_METADATA_ID, Language.ENGLISH);

        // Act.
        var feedback = service.generateNextCorrectAnswer(question.getQuestionId());

        // Assert.
        assertTrue(feedback.isCorrect());
        resetPersistenceContext();
        assertTrue(questionDataService.getQuestion(question.getQuestionId()).getInteractions().getLast().isCorrect());
    }

    /** Подсказку дала система, поэтому гипотез о рассуждении студента у неё нет. */
    @Test
    void generateNextCorrectAnswerRecordsNoHypotheses() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var question = service.generateQuestionByMetadata(
                TestData.TypeEvaluationBank.AVERAGE_OF_GRADES_METADATA_ID, Language.ENGLISH);

        // Act.
        var feedback = service.generateNextCorrectAnswer(question.getQuestionId());

        // Assert.
        assertTrue(feedback.isCorrect());
        assertEquals(1, feedback.getCorrectAnswers().length);
        assertArrayEquals(new Long[] { TestData.TypeEvaluationBank.LEN_SLOT, TestData.TypeEvaluationBank.INT_TYPE },
                feedback.getCorrectAnswers()[0].getAnswer());
        resetPersistenceContext();
        assertEquals(List.of(), recordedHypotheses(question.getQuestionId()));
    }

    // ---- уточняющий вопрос о рассуждении студента ----

    /** Ошибку, которую объясняют два заблуждения, сопровождает вопрос о причине с вариантами-заблуждениями. */
    @Test
    void addQuestionAnswerAsksClarificationForAmbiguousError() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var question = typeEvaluationQuestion();

        // Act.
        var feedback = answerIntegerForTrueDivision(question);

        // Assert.
        assertFalse(feedback.isCorrect());
        assertNotNull(feedback.getClarification());
        assertFalse(feedback.getClarification().prompt().isBlank());
        assertEquals(Set.of("operand_type", "c_style_division"), feedback.getClarification().options().stream()
                .map(ClarificationDto.Option::hypothesis)
                .collect(Collectors.toSet()));
    }

    /** Уточнение без ответа возвращается с перезагруженным вопросом, чтобы студент не пропустил его. */
    @Test
    void getQuestionReturnsClarificationAwaitingAnswer() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var question = typeEvaluationQuestion();
        var asked = answerIntegerForTrueDivision(question).getClarification();
        resetPersistenceContext();

        // Act.
        var reloaded = service.getQuestion(question.getQuestionId());

        // Assert.
        assertEquals(asked, reloaded.getFeedback().getClarification());
    }

    /** Перезагруженный вопрос сообщает уже принятые ответы: по ним интерфейс открывает следующие части выражения. */
    @Test
    void getQuestionReturnsAcceptedAnswers() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var question = typeEvaluationQuestion();
        var lengthAsInt = new AnswerDto(TestData.TypeEvaluationBank.LEN_SLOT, TestData.TypeEvaluationBank.INT_TYPE, true, null);
        service.addQuestionAnswer(new InteractionDto(question.getQuestionId(), new AnswerDto[] { lengthAsInt }));
        resetPersistenceContext();

        // Act.
        var reloaded = service.getQuestion(question.getQuestionId());

        // Assert.
        assertEquals(1, reloaded.getFeedback().getCorrectAnswers().length);
        assertArrayEquals(new Long[] { TestData.TypeEvaluationBank.LEN_SLOT, TestData.TypeEvaluationBank.INT_TYPE },
                reloaded.getFeedback().getCorrectAnswers()[0].getAnswer());
    }

    /**
     * Ошибку len(grades) → TypeError объясняют заблуждения разных навыков: путаница grades с grade и неприменимость len.
     * Пока студент не назвал причину, стратегии не засчитывается ни один навык, а после — навык выбранного заблуждения.
     */
    @Test
    void ambiguousErrorCountsLawsOfReasonNamedByStudent() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.TYPE_EVALUATION_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = gradeCountQuestion(attempt.getAttemptId());
        var lengthAsError = new AnswerDto(TestData.TypeEvaluationBank.LEN_SLOT, TestData.TypeEvaluationBank.ERROR_TYPE, true, null);
        var feedback = service.addQuestionAnswer(new InteractionDto(question.getQuestionId(), new AnswerDto[] { lengthAsError }));
        var countedBeforeReason = lastInteraction(attempt.getAttemptId()).violationLawNames();

        // Act.
        service.answerClarification(new ClarificationAnswerDto(question.getQuestionId(), optionOf(feedback, "variable_confused")));

        // Assert.
        assertEquals(Set.of("variable_confused", "inapplicable_assumed"), feedback.getClarification().options().stream()
                .map(ClarificationDto.Option::hypothesis)
                .collect(Collectors.toSet()));
        assertEquals(List.of(), countedBeforeReason);
        var counted = lastInteraction(attempt.getAttemptId());
        assertFalse(counted.isCorrect());
        assertEquals(List.of("operand_identification"), counted.violationLawNames());
    }

    /** Выбранная студентом причина записывается, и он получает объяснение именно этого заблуждения. */
    @Test
    void answerClarificationRecordsChosenHypothesisAndExplainsIt() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var question = typeEvaluationQuestion();
        var asked = answerIntegerForTrueDivision(question);

        // Act.
        var feedback = service.answerClarification(
                new ClarificationAnswerDto(question.getQuestionId(), optionOf(asked, "c_style_division")));

        // Assert.
        assertNotNull(feedback.explanation());
        resetPersistenceContext();
        assertEquals(List.of("c_style_division"), answeredClarifications(question.getQuestionId()));
        assertNull(service.getQuestion(question.getQuestionId()).getFeedback().getClarification());
    }

    /** «Другая причина» тоже закрывает уточнение, но гипотезу не подтверждает. */
    @Test
    void answerClarificationWithOtherReasonRecordsNoHypothesis() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var question = typeEvaluationQuestion();
        answerIntegerForTrueDivision(question);

        // Act.
        var feedback = service.answerClarification(new ClarificationAnswerDto(question.getQuestionId(), null));

        // Assert.
        assertNull(feedback.explanation());
        resetPersistenceContext();
        assertEquals(Arrays.asList((String) null), answeredClarifications(question.getQuestionId()));
    }

    /** Вариант, которого не было в вопросе, выбрать нельзя. */
    @Test
    void answerClarificationRejectsOptionNotOffered() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var question = typeEvaluationQuestion();
        var asked = answerIntegerForTrueDivision(question);
        var answer = new ClarificationAnswerDto(question.getQuestionId(), asked.getClarification().options().size());

        // Act & Assert.
        assertThrows(IllegalArgumentException.class, () -> service.answerClarification(answer));
    }

    // ---- уточнение рассуждения при верном ответе ----

    /** По умолчанию верный ответ не уточняется, даже если к нему ведёт и заблуждение. */
    @Test
    void correctAnswerReachedByMisconceptionIsNotClarifiedByDefault() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.TYPE_EVALUATION_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = gradeCountQuestion(attempt.getAttemptId());

        // Act.
        var feedback = answerProductAsInteger(question);

        // Assert.
        assertTrue(feedback.isCorrect());
        assertNull(feedback.getClarification());
    }

    /** Если стратегия спрашивает всегда, верный ответ, к которому ведёт и заблуждение, сопровождает вопрос о рассуждении. */
    @Test
    void correctAnswerReachedByMisconceptionIsClarifiedWhenStrategyAlwaysAsks() {
        // Arrange.
        setStrategySettings("{\"correctAnswerClarification\": {\"mode\": \"ALWAYS\"}}");
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.TYPE_EVALUATION_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = gradeCountQuestion(attempt.getAttemptId());

        // Act.
        var feedback = answerProductAsInteger(question);

        // Assert.
        assertTrue(feedback.isCorrect());
        assertNotNull(feedback.getClarification());
        assertEquals(Set.of("rule", "operand_type"), feedback.getClarification().options().stream()
                .map(ClarificationDto.Option::hypothesis)
                .collect(Collectors.toSet()));
    }

    /** До серии верных ответов заданной длины верный ответ уточняется, после — нет; серия считается до этого ответа. */
    @ParameterizedTest
    @CsvSource({
            "1, false",
            "2, true",
    })
    void correctAnswerIsClarifiedUntilStreakOfCorrectAnswers(int streakLength, boolean isClarified) {
        // Arrange.
        setStrategySettings("{\"correctAnswerClarification\": {\"mode\": \"UNTIL_STREAK\", \"streakLength\": "
                + streakLength + "}}");
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.TYPE_EVALUATION_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = gradeCountQuestion(attempt.getAttemptId());

        // Act.
        var feedback = answerProductAsInteger(question);

        // Assert.
        assertTrue(feedback.isCorrect());
        assertEquals(isClarified, feedback.getClarification() != null);
    }

    /** Выбравший заблуждение после верного ответа узнаёт, что ответ верен, а рассуждение ошибочно. */
    @Test
    void answerClarificationOfCorrectAnswerExplainsMisreasoning() {
        // Arrange.
        setStrategySettings("{\"correctAnswerClarification\": {\"mode\": \"ALWAYS\"}}");
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.TYPE_EVALUATION_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = gradeCountQuestion(attempt.getAttemptId());
        var asked = answerProductAsInteger(question);

        // Act.
        var feedback = service.answerClarification(new ClarificationAnswerDto(question.getQuestionId(), optionOf(asked, "operand_type")));

        // Assert.
        assertEquals("Выражение <code>len(grades) * grade</code> действительно имеет тип <code>int</code>, но рассуждение"
                + " ошибочно: тип результата задают правила арифметики, а не тип операнда, и оператор <code>*</code>"
                + " над целыми операндами даёт целое число.", feedback.explanation());
        resetPersistenceContext();
        assertEquals(List.of("operand_type"), answeredClarifications(question.getQuestionId()));
    }

    /**
     * Верный ответ продолжает серию, только если студент подтвердил верное рассуждение: заблуждение или
     * «другая причина» её обрывают, и следующий верный ответ снова уточняется.
     */
    @ParameterizedTest
    @CsvSource(value = {
            "rule,         false",
            "operand_type, true",
            "null,         true",
    }, nullValues = "null")
    void clarificationAnswerDecidesWhetherStreakContinues(String chosenHypothesis, boolean isNextClarified) {
        // Arrange.
        setStrategySettings("{\"correctAnswerClarification\": {\"mode\": \"ALWAYS\"}}");
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.TYPE_EVALUATION_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = gradeCountQuestion(attempt.getAttemptId());
        var asked = answerProductAsInteger(question);
        service.answerClarification(new ClarificationAnswerDto(question.getQuestionId(), optionOf(asked, chosenHypothesis)));
        setStrategySettings("{\"correctAnswerClarification\": {\"mode\": \"UNTIL_STREAK\", \"streakLength\": 1}}");
        resetPersistenceContext();

        // Act.
        var isClarified = strategyFactory.getStrategy(TestData.Exercises.STRATEGY_ID)
                .shouldClarifyCorrectAnswer(attempt.getAttemptId());

        // Assert.
        assertEquals(isNextClarified, isClarified);
    }

    // ---- оценка стратегией внутри попытки ----

    /** Верный ответ в попытке оценивается стратегией. */
    @Test
    void addQuestionAnswerInsideAttemptIsGradedByStrategy() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = service.generateQuestion(attempt.getAttemptId());
        var bankQuestion = TestData.ExpressionBank.byMetadataId(question.getQuestionMetadataId());

        // Act.
        var feedback = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(0)));

        // Assert.
        assertTrue(feedback.isCorrect());
        assertEquals(1f / 4 / TestData.Exercises.EXPRESSION_DT_QUESTIONS, feedback.getGrade(), GRADE_DELTA);
        assertEquals(Decision.CONTINUE, feedback.getStrategyDecision());
        assertEquals(AttemptStatus.INCOMPLETE, service.getExerciseAttempt(attempt.getAttemptId()).getStatus());
    }

    /** Ошибка в попытке: нулевая оценка. */
    @Test
    void wrongAnswerInsideAttemptGetsZeroGrade() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = service.generateQuestion(attempt.getAttemptId());
        var bankQuestion = TestData.ExpressionBank.byMetadataId(question.getQuestionMetadataId());

        // Act.
        var feedback = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(1)));

        // Assert.
        assertFalse(feedback.isCorrect());
        assertEquals(0f, feedback.getGrade(), GRADE_DELTA);
        assertEquals(Decision.CONTINUE, feedback.getStrategyDecision());
    }

    /** Верный ответ в домене на деревьях решений приносит стратегии применённые законы, а не только ошибки. */
    @Test
    void correctAnswerInDecisionTreeDomainRecordsAppliedLaws() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = service.generateQuestion(attempt.getAttemptId());
        var bankQuestion = TestData.ExpressionBank.byMetadataId(question.getQuestionMetadataId());

        // Act.
        service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(0)));

        // Assert.
        var answered = attemptDataService.getAttemptWithQuestions(attempt.getAttemptId()).questions().getFirst();
        assertFalse(answered.interactions().getLast().correctLawNames().isEmpty());
    }

    /** Подсказки не дают баллов. */
    @Test
    void hintsInsideAttemptDoNotRaiseGrade() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
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
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = service.generateQuestion(attempt.getAttemptId());

        // Act.
        var feedback = solveByAnswers(question, TestData.ExpressionBank.byMetadataId(question.getQuestionMetadataId()));

        // Assert.
        assertEquals(0, feedback.getStepsLeft());
        assertEquals(Decision.CONTINUE, feedback.getStrategyDecision());
        var inProgress = service.getExerciseAttempt(attempt.getAttemptId());
        assertEquals(AttemptStatus.INCOMPLETE, inProgress.getStatus());
        assertTrue(inProgress.getQuestionIds().length < TestData.Exercises.EXPRESSION_DT_QUESTIONS);
        assertNotNull(service.getExistingExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null));
    }

    /** Решены все вопросы стадии: попытка завершена. */
    @Test
    void completingEveryStageQuestionFinishesAttempt() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var first = service.generateQuestion(attempt.getAttemptId());
        var firstBankQuestion = TestData.ExpressionBank.byMetadataId(first.getQuestionMetadataId());
        solveByAnswers(first, firstBankQuestion);
        var second = service.generateQuestion(attempt.getAttemptId());
        var secondBankQuestion = TestData.ExpressionBank.byMetadataId(second.getQuestionMetadataId());

        // Act.
        var feedback = solveByAnswers(second, secondBankQuestion);

        // Assert.
        assertEquals(Decision.FINISH, feedback.getStrategyDecision());
        assertEquals((firstBankQuestion.steps() + secondBankQuestion.steps()) / 4f / TestData.Exercises.EXPRESSION_DT_QUESTIONS,
                feedback.getGrade(), GRADE_DELTA);
        var finished = service.getExerciseAttempt(attempt.getAttemptId());
        assertEquals(AttemptStatus.COMPLETED_BY_USER, finished.getStatus());
        assertArrayEquals(new Long[] { first.getQuestionId(), second.getQuestionId() }, finished.getQuestionIds());
        assertNull(service.getExistingExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null));
    }

    // ---- дополнительные вопросы ----

    /** После ошибки выдаётся доп. вопрос. */
    @Test
    void generateSupplementaryQuestionAfterMistakeReturnsQuestion() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.PARENTHESES_AND_UNARY_MINUS;
        var question = attemptlessQuestion(bankQuestion);
        var mistake = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(1)));

        // Act.
        var supplementary = service.generateSupplementaryQuestion(question.getQuestionId(), violationLawsOf(mistake));

        // Assert.
        var supplementaryQuestion = supplementaryQuestion(supplementary);
        assertNotEquals(question.getQuestionId(), supplementaryQuestion.getQuestionId());
        assertFalse(supplementaryQuestion.getText().isBlank());
        assertTrue(supplementaryQuestion.getAnswers().length > 0);
        assertEquals(supplementaryQuestion.getQuestionId(), service.getQuestion(supplementaryQuestion.getQuestionId()).getQuestionId());
    }

    /** Ответ на доп. вопрос оценивается, и цепочка продолжается. */
    @Test
    void addSupplementaryQuestionAnswerReturnsFeedbackAndAdvancesChain() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.PARENTHESES_AND_UNARY_MINUS;
        var question = attemptlessQuestion(bankQuestion);
        var laws = violationLawsOf(service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(1))));
        var supplementary = supplementaryQuestion(service.generateSupplementaryQuestion(question.getQuestionId(), laws));

        // Act.
        var feedback = service.addSupplementaryQuestionAnswer(anyAnswer(supplementary));

        // Assert.
        assertNotNull(feedback.getAction());
        assertFalse(feedback.getMessage().getMessage().isBlank());
        assertDoesNotThrow(() -> service.generateSupplementaryQuestion(question.getQuestionId(), laws));
    }

    /** Переключатели доп. вопроса с множественным выбором читаются как значения, а не как объекты ответа. */
    @Test
    void multiChoiceSupplementaryAnswerIsReadAsSwitchValues() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.MEMBER_ACCESS_PLUS;
        var question = attemptlessQuestion(bankQuestion);
        var laws = violationLawsOf(service.addQuestionAnswer(interaction(question, bankQuestion.endEvaluationAnswerId())));
        var supplementary = supplementaryQuestion(service.generateSupplementaryQuestion(question.getQuestionId(), laws));
        var everythingSwitchedOn = new InteractionDto(supplementary.getQuestionId(), Arrays.stream(supplementary.getAnswers())
                .map(answer -> new AnswerDto(answer.getId(), (long) MultiChoiceOptionsData.SWITCH_ON, true, null))
                .toArray(AnswerDto[]::new));

        // Act.
        var feedback = service.addSupplementaryQuestionAnswer(everythingSwitchedOn);

        // Assert.
        assertEquals(MULTI_CHOICE, supplementary.getType());
        assertEquals(FeedbackDto.MessageType.SUCCESS, feedback.getMessage().getType());
    }

    /** Обычный вопрос за доп. вопрос не принимается. */
    @Test
    void addSupplementaryQuestionAnswerForOrdinaryQuestionFails() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        var bankQuestion = TestData.ExpressionBank.MEMBER_ACCESS_PLUS;
        var question = attemptlessQuestion(bankQuestion);

        // Act & Assert.
        assertThrows(IllegalArgumentException.class,
                () -> service.addSupplementaryQuestionAnswer(interaction(question, bankQuestion.operatorAt(0))));
    }

    /** Доп. вопрос не попадает в список вопросов попытки. */
    @Test
    void supplementaryQuestionInsideAttemptDoesNotBecomeAttemptQuestion() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        var attempt = service.createExerciseAttempt(TestData.Exercises.EXPRESSION_DT_ID, TestData.Users.GLOBAL_STUDENT_ID, null);
        var question = service.generateQuestion(attempt.getAttemptId());
        var bankQuestion = TestData.ExpressionBank.byMetadataId(question.getQuestionMetadataId());
        var mistake = service.addQuestionAnswer(interaction(question, bankQuestion.operatorAt(1)));

        // Act.
        var supplementary = service.generateSupplementaryQuestion(question.getQuestionId(), violationLawsOf(mistake));

        // Assert.
        supplementaryQuestion(supplementary);
        assertArrayEquals(new Long[] { question.getQuestionId() }, service.getExerciseAttempt(attempt.getAttemptId()).getQuestionIds());
    }

    // ---- вспомогательное ----

    private QuestionDto attemptlessQuestion(BankQuestion bankQuestion) {
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

    /** Сброс кэша контекста. */
    private void resetPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
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

    private static QuestionDto supplementaryQuestion(SupplementaryQuestionDto supplementary) {
        return assertInstanceOf(SupplementaryQuestionDto.Question.class, supplementary).question();
    }

    private static InteractionDto anyAnswer(QuestionDto question) {
        var right = question instanceof MatchingQuestionDto matching ? matching.getGroups()[0].getId() : question.getAnswers()[0].getId();
        return new InteractionDto(question.getQuestionId(), Arrays.stream(question.getAnswers())
                .map(answer -> new AnswerDto(answer.getId(), right, true, null))
                .toArray(AnswerDto[]::new));
    }

    // Номер варианта с этой гипотезой в заданном вопросе; без гипотезы — «другая причина».
    private static Integer optionOf(FeedbackDto feedback, String hypothesis) {
        return hypothesis == null ? null : feedback.getClarification().options().stream()
                .filter(option -> option.hypothesis().equals(hypothesis))
                .map(ClarificationDto.Option::id)
                .findFirst()
                .orElseThrow();
    }

    private QuestionDto typeEvaluationQuestion() {
        return service.generateQuestionByMetadata(
                TestData.TypeEvaluationBank.AVERAGE_OF_GRADES_METADATA_ID, Language.ENGLISH);
    }

    /** Верный тип len(grades), затем int для total / len(grades): ошибка, которую объясняют два заблуждения. */
    private FeedbackDto answerIntegerForTrueDivision(QuestionDto question) {
        var lengthAsInt = new AnswerDto(TestData.TypeEvaluationBank.LEN_SLOT, TestData.TypeEvaluationBank.INT_TYPE, true, null);
        var afterLength = service.addQuestionAnswer(
                new InteractionDto(question.getQuestionId(), new AnswerDto[] { lengthAsInt }));
        assertTrue(afterLength.isCorrect());
        var divisionAsInt = new AnswerDto(TestData.TypeEvaluationBank.DIV_SLOT, TestData.TypeEvaluationBank.INT_TYPE, true, null);
        return service.addQuestionAnswer(new InteractionDto(question.getQuestionId(),
                Stream.concat(Arrays.stream(afterLength.getCorrectAnswers()), Stream.of(divisionAsInt))
                        .toArray(AnswerDto[]::new)));
    }

    private void setStrategySettings(String settings) {
        var exercise = entityManager.find(ExerciseEntity.class, TestData.Exercises.TYPE_EVALUATION_ID);
        try {
            exercise.setOptions(exercise.getOptions().withStrategySettings(new ObjectMapper().readValue(settings,
                    new TypeReference<Map<String, Object>>() {
                    })));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
        entityManager.flush();
    }

    private AttemptQuestionInteractionData lastInteraction(long attemptId) {
        return attemptDataService.getAttemptWithQuestions(attemptId).questions().getLast().interactions().getLast();
    }

    private QuestionDto gradeCountQuestion(long attemptId) {
        var question = service.generateQuestion(attemptId);
        assertEquals(TestData.TypeEvaluationBank.GRADE_COUNT_METADATA_ID, question.getQuestionMetadataId());
        return question;
    }

    // len(grades) — int без заблуждений, затем len(grades) * grade — int, к которому ведёт и заблуждение «тип операнда».
    private FeedbackDto answerProductAsInteger(QuestionDto question) {
        var lengthAsInt = new AnswerDto(TestData.TypeEvaluationBank.LEN_SLOT, TestData.TypeEvaluationBank.INT_TYPE, true, null);
        var afterLength = service.addQuestionAnswer(
                new InteractionDto(question.getQuestionId(), new AnswerDto[] { lengthAsInt }));
        assertTrue(afterLength.isCorrect());
        assertNull(afterLength.getClarification());
        var productAsInt = new AnswerDto(TestData.TypeEvaluationBank.MUL_SLOT, TestData.TypeEvaluationBank.INT_TYPE, true, null);
        return service.addQuestionAnswer(new InteractionDto(question.getQuestionId(),
                Stream.concat(Arrays.stream(afterLength.getCorrectAnswers()), Stream.of(productAsInt))
                        .toArray(AnswerDto[]::new)));
    }

    private List<String> answeredClarifications(long questionId) {
        return questionDataService.getQuestion(questionId).getInteractions().stream()
                .filter(interaction -> interaction.getClarification() != null && interaction.getClarification().isAnswered())
                .map(interaction -> {
                    var chosen = interaction.getClarification().chosenReasoning();
                    return chosen == null ? null : interaction.getReasonings().get(chosen).hypothesis();
                })
                .toList();
    }

    private List<String> recordedHypotheses(long questionId) {
        return questionDataService.getQuestion(questionId).getInteractions().stream()
                .flatMap(interaction -> interaction.getReasonings().stream())
                .filter(reasoning -> reasoning.hypothesis() != null)
                .map(reasoning -> reasoning.hypothesis() + ":" + reasoning.isCorrect())
                .toList();
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
