package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionBankControllerAuthorizationTest extends AbstractAuthorizationTest {

    private static String searchRequest(Long courseId) {
        return """
                {
                  "domainId": "%s",
                  "complexity": 0.5,
                  "tags": [],
                  "laws": [],
                  "concepts": [],
                  "skills": [],
                  "limit": 5,
                  "courseId": %s
                }
                """.formatted(TestData.DOMAIN_ID, courseId == null ? "null" : courseId.toString());
    }


    /** Поиск по банку требует VIEW_EXERCISE в GLOBAL-области. */
    @Test
    void searchForbiddenForGlobalStudent() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequest(null)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Без courseId проверка идёт в GLOBAL, где прав у преподавателя нет. */
    @Test
    void searchForbiddenForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequest(null)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Без ролей поиск закрыт. */
    @Test
    void searchForbiddenForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.USER_WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequest(null)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** В контексте своего курса поиск преподавателю доступен. */
    @Test
    void searchAllowedForCourseTeacherInOwnCourse() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequest(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Контекст чужого курса прав не даёт. */
    @Test
    void searchForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequest(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** У студента VIEW_EXERCISE нет и в своём курсе. */
    @Test
    void searchForbiddenForCourseStudentInOwnCourse() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequest(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }
}
