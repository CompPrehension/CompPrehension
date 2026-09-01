package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsersControllerAuthorizationTest extends AbstractAuthorizationTest {

    /** Флаг пула держится на VIEW_EXERCISE в GLOBAL-области. */
    @Test
    void whoamiAllowsGlobalPoolForGlobalExerciseAuthor() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(get("/api/users/whoami"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canViewGlobalPool").value(true));
    }

    /** Администратору доступен и пул. */
    @Test
    void whoamiAllowsGlobalPoolForGlobalAdmin() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_ADMIN_ID);

        // Act.
        var result = mockMvc.perform(get("/api/users/whoami"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canViewGlobalPool").value(true));
    }

    /** У студента VIEW_EXERCISE нет. */
    @Test
    void whoamiDeniesGlobalPoolForGlobalStudent() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/users/whoami"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canViewGlobalPool").value(false));
    }

    /** Права в курсе глобальный пул не открывают. */
    @Test
    void whoamiDeniesGlobalPoolForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/users/whoami"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canViewGlobalPool").value(false));
    }

    /** Без ролей вызов доступен, но флаги пусты. */
    @Test
    void whoamiIsAvailableForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.USER_WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(get("/api/users/whoami"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canViewGlobalPool").value(false));
    }

    /** Ответ описывает пришедшего пользователя. */
    @Test
    void whoamiDescribesCurrentUser() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/users/whoami"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TestData.MAIN_COURSE_STUDENT_ID))
                .andExpect(jsonPath("$.email").value("main-course-student@test.local"));
    }
}
