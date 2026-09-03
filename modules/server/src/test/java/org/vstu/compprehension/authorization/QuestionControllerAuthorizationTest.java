package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.vstu.compprehension.Service.ExerciseAttemptService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionControllerAuthorizationTest extends AbstractAuthorizationTest {

    @Autowired private ExerciseAttemptService exerciseAttemptService;

    /** Вопрос принадлежит чужой попытке. */
    @Test
    void getQuestionForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var question = createQuestion(createMainCourseAttempt());
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/question")
                .param("questionId", String.valueOf(question.getId())));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Привилегия на чужие вопросы держится на EDIT_EXERCISE. */
    @Test
    void getQuestionForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        var question = createQuestion(createMainCourseAttempt());
        actingAs(TestData.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/question")
                .param("questionId", String.valueOf(question.getId())));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Ответ в чужое решение не отправить. */
    @Test
    void addQuestionAnswerForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var question = createQuestion(createMainCourseAttempt());
        actingAs(TestData.GLOBAL_STUDENT_ID);
        var body = "{\"questionId\": " + question.getId() + ", \"answers\": []}";

        // Act.
        var result = mockMvc.perform(post("/api/question/addQuestionAnswer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Дополнительные вопросы защищены так же. */
    @Test
    void addSupplementaryQuestionAnswerForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var question = createQuestion(createMainCourseAttempt());
        actingAs(TestData.GLOBAL_STUDENT_ID);
        var body = "{\"questionId\": " + question.getId() + ", \"answers\": []}";

        // Act.
        var result = mockMvc.perform(post("/api/question/addSupplementaryQuestionAnswer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Запрос дополнительного вопроса по чужому закрыт. */
    @Test
    void generateSupplementaryQuestionForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        var question = createQuestion(attempt);
        actingAs(TestData.GLOBAL_STUDENT_ID);
        var body = "{\"questionId\": " + question.getId()
                + ", \"exerciseAttemptId\": " + attempt.getId() + ", \"violationLaws\": []}";

        // Act.
        var result = mockMvc.perform(post("/api/question/generateSupplementaryQuestion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Подсказка с ответом по чужому вопросу закрыта. */
    @Test
    void generateNextCorrectAnswerForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var question = createQuestion(createMainCourseAttempt());
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/question/generateNextCorrectAnswer")
                .param("questionId", String.valueOf(question.getId())));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Генерация идёт в чужую попытку. */
    @Test
    void generateQuestionForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/question/generate")
                .param("attemptId", String.valueOf(attempt.getId())));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Ассистент чужую попытку не продолжает. */
    @Test
    void generateQuestionForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/question/generate")
                .param("attemptId", String.valueOf(attempt.getId())));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Генерация по метаданным требует EDIT_EXERCISE в GLOBAL-области. */
    @Test
    void generateByMetadataForbiddenForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/question/generateByMetadata")
                .param("metadataId", "1"));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Студенту генерация по метаданным закрыта. */
    @Test
    void generateByMetadataForbiddenForGlobalStudent() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/question/generateByMetadata")
                .param("metadataId", "1"));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Студенту недоступен вопрос без попытки. */
    @Test
    void getQuestionForbiddenForAttemptlessQuestionAndGlobalStudent() throws Exception {
        // Arrange.
        var question = createQuestionWithoutAttempt();
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/question")
                .param("questionId", String.valueOf(question.getId())));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Преподавателю курса недоступен вопрос без попытки. */
    @Test
    void generateNextCorrectAnswerForbiddenForAttemptlessQuestionAndCourseTeacher() throws Exception {
        // Arrange.
        var question = createQuestionWithoutAttempt();
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/question/generateNextCorrectAnswer")
                .param("questionId", String.valueOf(question.getId())));

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
                .ensureCanAccessQuestion(TestData.GLOBAL_EXERCISE_AUTHOR_ID, question.getId()));
    }
}
