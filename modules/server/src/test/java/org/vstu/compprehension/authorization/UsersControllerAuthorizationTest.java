package org.vstu.compprehension.authorization;

import org.vstu.compprehension.controllers.UsersController;
import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

class UsersControllerAuthorizationTest extends AbstractAuthorizationTest {

    /** Флаг пула держится на VIEW_GLOBAL_POOL. */
    @Test
    void whoamiAllowsGlobalPoolForGlobalExerciseAuthor() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(UsersController.class)
                .getAll()).build().toUri()));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canViewGlobalPool").value(true));
    }

    /** Администратору доступен и пул. */
    @Test
    void whoamiAllowsGlobalPoolForGlobalAdmin() throws Exception {
        // Arrange.
        actingAs(TestData.Users.ADMIN_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(UsersController.class)
                .getAll()).build().toUri()));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canViewGlobalPool").value(true));
    }

    /** У студента VIEW_GLOBAL_POOL нет. */
    @Test
    void whoamiDeniesGlobalPoolForGlobalStudent() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(UsersController.class)
                .getAll()).build().toUri()));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canViewGlobalPool").value(false));
    }

    /** VIEW_GLOBAL_POOL из роли в курсе открывает глобальный пул. */
    @Test
    void whoamiAllowsGlobalPoolForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(UsersController.class)
                .getAll()).build().toUri()));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canViewGlobalPool").value(true));
    }

    /** У ассистента курса глобального пула нет. */
    @Test
    void whoamiDeniesGlobalPoolForCourseAssistant() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(UsersController.class)
                .getAll()).build().toUri()));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canViewGlobalPool").value(false));
    }

    /** Без ролей вызов доступен, но флаги пусты. */
    @Test
    void whoamiIsAvailableForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.Users.WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(UsersController.class)
                .getAll()).build().toUri()));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canViewGlobalPool").value(false));
    }

    /** Ответ описывает пришедшего пользователя. */
    @Test
    void whoamiDescribesCurrentUser() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(UsersController.class)
                .getAll()).build().toUri()));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TestData.Users.MAIN_COURSE_STUDENT_ID))
                .andExpect(jsonPath("$.email").value("main-course-student@test.local"));
    }
}
