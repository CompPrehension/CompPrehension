package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LtiDeepLinkingControllerAuthorizationTest extends AbstractAuthorizationTest {

    private static final String BUILD_REQUEST = "{\"exerciseIds\": [%d]}".formatted(TestData.INHERITED_EXERCISE_ID);

    @AfterEach
    void resetLtiContext() {
        TestLtiContextProvider.reset();
    }

    /** Сборка активностей меняет состав курса в LMS, поэтому требует EDIT_COURSE. */
    @Test
    void buildForbiddenForCourseStudent() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.MAIN_COURSE_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BUILD_REQUEST));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Ассистент видит курс, но не меняет его состав. */
    @Test
    void buildForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.MAIN_COURSE_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BUILD_REQUEST));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /**
     * Права проверяются в том курсе, из которого пришёл запуск, а не в том, где они у
     * пользователя есть: преподаватель соседнего курса ничего не соберёт.
     */
    @Test
    void buildForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.MAIN_COURSE_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BUILD_REQUEST));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Глобальные права не подменяют членство в курсе: администратор LMS здесь ни при чём. */
    @Test
    void buildForbiddenForGlobalExerciseAuthor() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.MAIN_COURSE_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BUILD_REQUEST));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Список уже добавленных активностей закрыт тем же правом, что и сборка. */
    @Test
    void existingForbiddenForCourseStudent() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.MAIN_COURSE_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/lti/deep-link/existing"));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /**
     * Без deep-linking-сессии до проверки прав дело не доходит вовсе: эндпоинт отвечает
     * отказом по некорректному запросу, а не по правам. Фиксирует порядок проверок.
     */
    @Test
    void buildRejectedWithoutDeepLinkingSession() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.MAIN_COURSE_EXTERNAL_ID);
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BUILD_REQUEST));

        // Assert.
        result.andExpect(status().isBadRequest());
    }

    /** Без LTI-контекста курс определить не из чего, поэтому запрос некорректен. */
    @Test
    void buildRejectedWithoutLtiContext() throws Exception {
        // Arrange.
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BUILD_REQUEST));

        // Assert.
        result.andExpect(status().isBadRequest());
    }
}
