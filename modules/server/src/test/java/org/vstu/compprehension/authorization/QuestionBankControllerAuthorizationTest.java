package org.vstu.compprehension.authorization;

import org.vstu.compprehension.frontend.dto.QuestionBankSearchRequestDto;
import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionBankControllerAuthorizationTest extends AbstractAuthorizationTest {

    private static QuestionBankSearchRequestDto searchRequest(Long courseId) {
        return QuestionBankSearchRequestDto.builder()
                .domainId(TestData.Exercises.DOMAIN_ID)
                .complexity(0.5f)
                .tags(List.of())
                .laws(List.of())
                .concepts(List.of())
                .skills(List.of())
                .limit(5)
                .courseId(courseId)
                .build();
    }

    /** Поиск по банку вне курса требует VIEW_GLOBAL_POOL. */
    @Test
    void searchForbiddenForGlobalStudent() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(searchRequest(null))));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Без courseId поиск идёт по VIEW_GLOBAL_POOL, который есть у преподавателя. */
    @Test
    void searchInGlobalPoolAllowedForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(searchRequest(null))));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** У ассистента VIEW_GLOBAL_POOL нет. */
    @Test
    void searchInGlobalPoolForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(searchRequest(null))));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Без ролей поиск закрыт. */
    @Test
    void searchForbiddenForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.Users.WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(searchRequest(null))));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** В контексте своего курса поиск преподавателю доступен. */
    @Test
    void searchAllowedForCourseTeacherInOwnCourse() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(searchRequest(TestData.Courses.MAIN_ID))));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Контекст чужого курса прав не даёт. */
    @Test
    void searchForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.Users.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(searchRequest(TestData.Courses.MAIN_ID))));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** У студента SEARCH_QUESTION_BANK нет и в своём курсе. */
    @Test
    void searchForbiddenForCourseStudentInOwnCourse() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(post("/api/question-bank/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(searchRequest(TestData.Courses.MAIN_ID))));

        // Assert.
        result.andExpect(status().isForbidden());
    }
}
