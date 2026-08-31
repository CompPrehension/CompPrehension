package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourseControllerAuthorizationTest extends AbstractAuthorizationTest {

    /** Глобальный администратор видит все курсы системы, а не только те, где у него роль. */
    @Test
    void myCoursesReturnsAllCoursesForGlobalAdmin() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_ADMIN_ID);

        // Act.
        var result = mockMvc.perform(get("/api/course/my"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    /** Здесь авторизация выражена фильтрацией: преподаватель получает только свой курс. */
    @Test
    void myCoursesReturnsOnlyOwnCourseForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/course/my"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(TestData.MAIN_COURSE_ID));
    }

    /** Глобальная роль не даёт членства ни в одном курсе: список пуст. */
    @Test
    void myCoursesIsEmptyForGlobalExerciseAuthor() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(get("/api/course/my"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /** Без ролей курсов не видно. */
    @Test
    void myCoursesIsEmptyForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.USER_WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(get("/api/course/my"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * TODO: поправить поведение.
     * <p>
     * По задуманной модели студент курсов не листает — у роли STUDENT нет VIEW_COURSE. Но
     * {@code api/course/my} прав не проверяет, а фильтрует по наличию любой роли в курсе,
     * поэтому свой курс студент всё же видит. Здесь должен остаться пустой ответ либо отказ.
     */
    @Test
    void myCoursesAreListedForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/course/my"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /** Список курсов, куда включено упражнение, — инструмент ведущего пул. */
    @Test
    void membershipsAllowedForGlobalExerciseAuthor() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(get("/api/course/memberships")
                .param("exerciseId", String.valueOf(TestData.INHERITED_EXERCISE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /**
     * Эндпоинт спрашивает VIEW_EXERCISE в GLOBAL-области, поэтому прав преподавателя в курсе для него недостаточно.
     */
    @Test
    void membershipsForbiddenForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/course/memberships")
                .param("exerciseId", String.valueOf(TestData.INHERITED_EXERCISE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Студенту список курсов упражнения закрыт. */
    @Test
    void membershipsForbiddenForGlobalStudent() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/course/memberships")
                .param("exerciseId", String.valueOf(TestData.INHERITED_EXERCISE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Импорт упражнения из пула наследованием меняет состав курса, поэтому требует EDIT_COURSE. */
    @Test
    void addExerciseToCourseAllowedForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/course/exercise/add")
                .param("exerciseId", String.valueOf(TestData.GLOBAL_POOL_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** У ассистента EDIT_COURSE нет, состав курса он не меняет. */
    @Test
    void addExerciseToCourseForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(post("/api/course/exercise/add")
                .param("exerciseId", String.valueOf(TestData.GLOBAL_POOL_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** EDIT_COURSE действует только в своём курсе. */
    @Test
    void addExerciseToCourseForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/course/exercise/add")
                .param("exerciseId", String.valueOf(TestData.GLOBAL_POOL_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /**
     * Наследовать можно только упражнение из общего пула. Права есть, отказ даёт состояние.
     */
    @Test
    void addExerciseToCourseRejectedForPrivateExerciseOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/course/exercise/add")
                .param("exerciseId", String.valueOf(TestData.OTHER_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isConflict());
    }

    /** Отвязка наследованного упражнения тоже меняет состав курса. */
    @Test
    void removeExerciseFromCourseAllowedForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(delete("/api/course/exercise/remove")
                .param("exerciseId", String.valueOf(TestData.INHERITED_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Ассистент не отвязывает упражнения от курса. */
    @Test
    void removeExerciseFromCourseForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(delete("/api/course/exercise/remove")
                .param("exerciseId", String.valueOf(TestData.INHERITED_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Преподаватель чужого курса не может вычистить состав соседнего. */
    @Test
    void removeExerciseFromCourseForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(delete("/api/course/exercise/remove")
                .param("exerciseId", String.valueOf(TestData.INHERITED_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }
}
