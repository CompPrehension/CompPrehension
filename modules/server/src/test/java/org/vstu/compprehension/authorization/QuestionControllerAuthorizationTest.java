package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionControllerAuthorizationTest extends AbstractAuthorizationTest {

    /** Вопрос принадлежит попытке, а попытка — студенту: посторонний его не читает. */
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

    /** Ассистенту чужие вопросы закрыты: привилегия выводится из EDIT_EXERCISE, которого у него нет. */
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

    /** Отправка ответа на чужой вопрос — попытка вмешаться в чужое решение. */
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

    /** Дополнительные вопросы принадлежат тому же решению и защищены так же. */
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

    /** Запрос дополнительного вопроса по чужому вопросу закрыт. */
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

    /** Подсказка с правильным ответом на чужой вопрос — тем более не для посторонних. */
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

    /** Генерация очередного вопроса идёт в чужую попытку, поэтому закрыта. */
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

    /** Ассистент не может продолжать чужую попытку. */
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

    /**
     * Генерация вопроса по метаданным — инструмент ведения общего банка, поэтому спрашивает
     * EDIT_EXERCISE именно в GLOBAL-области. Прав преподавателя в своём курсе не хватает.
     */
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
}
