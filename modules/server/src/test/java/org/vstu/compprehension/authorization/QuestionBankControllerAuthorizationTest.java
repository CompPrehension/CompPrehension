package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionBankControllerAuthorizationTest extends AbstractAuthorizationTest {

    private static final String SEARCH_REQUEST = """
            {
              "domainId": "%s",
              "complexity": 0.5,
              "tags": [],
              "laws": [],
              "concepts": [],
              "skills": [],
              "limit": 5
            }
            """.formatted(TestData.DOMAIN_ID);

    /** Поиск по общему банку вопросов спрашивает VIEW_EXERCISE в GLOBAL-области: студенту закрыт. */
    @Test
    void searchForbiddenForGlobalStudent() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SEARCH_REQUEST));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Банк вопросов общий, поэтому прав преподавателя в отдельном курсе для поиска недостаточно. */
    @Test
    void searchForbiddenForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SEARCH_REQUEST));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Без ролей поиск по банку закрыт. */
    @Test
    void searchForbiddenForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.USER_WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SEARCH_REQUEST));

        // Assert.
        result.andExpect(status().isForbidden());
    }
}
