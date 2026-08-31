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


    /** Поиск по общему банку вопросов спрашивает VIEW_EXERCISE в GLOBAL-области: студенту закрыт. */
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

    /** Без контекста курса поиск проверяется глобально, а глобальных прав у преподавателя нет. */
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

    /** Без ролей поиск по банку закрыт. */
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

    /**
     * Поиск в контексте своего курса доступен преподавателю
     */
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

    /** Студенту банк закрыт и в контексте своего курса: VIEW_EXERCISE у роли нет. */
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
