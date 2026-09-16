package org.vstu.compprehension.authorization;

import org.vstu.compprehension.controllers.QuestionController;
import org.vstu.compprehension.frontend.dto.AnswerDto;
import org.vstu.compprehension.frontend.dto.InteractionDto;
import org.vstu.compprehension.frontend.dto.SupplementaryQuestionRequestDto;
import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.vstu.compprehension.services.ExerciseAttemptDataService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodName;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

class QuestionControllerAuthorizationTest extends AbstractAuthorizationTest {

    @Autowired private ExerciseAttemptDataService exerciseAttemptService;

    /** Вопрос принадлежит чужой попытке. */
    @Test
    void getQuestionForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var question = createQuestion(createMainCourseAttempt());
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(QuestionController.class)
                .getQuestion(question.getId())).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Привилегия на чужие вопросы держится на EDIT_EXERCISE. */
    @Test
    void getQuestionForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        var question = createQuestion(createMainCourseAttempt());
        actingAs(TestData.Users.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(QuestionController.class)
                .getQuestion(question.getId())).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Ответ в чужое решение не отправить. */
    @Test
    void addQuestionAnswerForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var question = createQuestion(createMainCourseAttempt());
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);
        var interaction = InteractionDto.builder()
                .questionId(question.getId())
                .answers(new AnswerDto[0])
                .build();

        // Act.
        var result = mockMvc.perform(post(fromMethodCall(on(QuestionController.class)
                        .addQuestionAnswer(interaction)).build().toUri())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(interaction)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Дополнительные вопросы защищены так же. */
    @Test
    void addSupplementaryQuestionAnswerForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var question = createQuestion(createMainCourseAttempt());
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);
        var interaction = InteractionDto.builder()
                .questionId(question.getId())
                .answers(new AnswerDto[0])
                .build();

        // Act.
        var result = mockMvc.perform(post(fromMethodCall(on(QuestionController.class)
                        .addSupplementaryQuestionAnswer(interaction)).build().toUri())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(interaction)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Запрос дополнительного вопроса по чужому закрыт. */
    @Test
    void generateSupplementaryQuestionForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        var question = createQuestion(attempt);
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);
        var questionRequest = SupplementaryQuestionRequestDto.builder()
                .questionId(question.getId())
                .exerciseAttemptId(attempt.getId())
                .violationLaws(new String[0])
                .build();

        // Act.
        var result = mockMvc.perform(post(fromMethodName(QuestionController.class, "generateSupplementaryQuestion", questionRequest).build().toUri())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(questionRequest)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Подсказка с ответом по чужому вопросу закрыта. */
    @Test
    void generateNextCorrectAnswerForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var question = createQuestion(createMainCourseAttempt());
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(QuestionController.class)
                .generateNextCorrectAnswer(question.getId())).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Генерация идёт в чужую попытку. */
    @Test
    void generateQuestionForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(QuestionController.class)
                .generateQuestion(attempt.getId())).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Ассистент чужую попытку не продолжает. */
    @Test
    void generateQuestionForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.Users.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(QuestionController.class)
                .generateQuestion(attempt.getId())).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Генерация по метаданным требует EDIT_EXERCISE в GLOBAL-области. */
    @Test
    void generateByMetadataForbiddenForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(QuestionController.class)
                .generateQuestionByMetadata(1)).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Студенту генерация по метаданным закрыта. */
    @Test
    void generateByMetadataForbiddenForGlobalStudent() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(QuestionController.class)
                .generateQuestionByMetadata(1)).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Студенту недоступен вопрос без попытки. */
    @Test
    void getQuestionForbiddenForAttemptlessQuestionAndGlobalStudent() throws Exception {
        // Arrange.
        var question = createQuestionWithoutAttempt();
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(QuestionController.class)
                .getQuestion(question.getId())).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Преподавателю курса недоступен вопрос без попытки. */
    @Test
    void generateNextCorrectAnswerForbiddenForAttemptlessQuestionAndCourseTeacher() throws Exception {
        // Arrange.
        var question = createQuestionWithoutAttempt();
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(QuestionController.class)
                .generateNextCorrectAnswer(question.getId())).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Автору пула вопрос без попытки открыт */
    @Test
    void attemptlessQuestionIsAccessibleToGlobalAuthor() {
        // Arrange.
        var question = createQuestionWithoutAttempt();

        // Act & Assert.
        assertDoesNotThrow(() -> exerciseAttemptService
                .ensureCanAccessQuestion(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID, question.getId()));
    }
}
