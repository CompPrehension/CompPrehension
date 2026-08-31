package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsersControllerAuthorizationTest extends AbstractAuthorizationTest {

    /** Глобальный пул виден тому, у кого есть VIEW_EXERCISE в GLOBAL-области. */
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

    /** Администратору доступно всё, включая пул. */
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

    /** Студент пула не видит: у роли остался только SOLVE_EXERCISE. */
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

    /** Права в курсе не открывают глобальный пул: области не сообщаются. */
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

    /** Без ролей флаги пусты, но сам вызов доступен — иначе интерфейс не отрисуется. */
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

    /** Ответ описывает того, кто пришёл, а не кого-то другого. */
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
