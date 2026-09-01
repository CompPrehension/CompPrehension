package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseControllerAuthorizationTest extends AbstractAuthorizationTest {

    /** Параметры упражнения нужны для решения, студенту курса доступны. */
    @Test
    void shortInfoAllowedForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/shortInfo")
                .param("id", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Права преподавателя не переносятся в соседний курс. */
    @Test
    void shortInfoForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/shortInfo")
                .param("id", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Без ролей доступа нет. */
    @Test
    void shortInfoForbiddenForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.USER_WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/shortInfo")
                .param("id", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Приватное упражнение курса вне контекста курса недоступно. */
    @Test
    void shortInfoOfCourseExerciseRejectedWithoutCourseContext() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/shortInfo")
                .param("id", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID)));

        // Assert.
        result.andExpect(status().isConflict());
    }

    /** По приватному упражнению курса нельзя завести попытку вне контекста курса. */
    @Test
    void attemptOnCourseExerciseRejectedWithoutCourseContext() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/createExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID)));

        // Assert.
        result.andExpect(status().isConflict());
    }

    /** Поиск своей незавершённой попытки студенту курса разрешён. */
    @Test
    void getExistingAttemptAllowedForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExistingExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Чужой курс закрыт и на чтение попыток. */
    @Test
    void getExistingAttemptForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExistingExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Студент начинает решать упражнение своего курса. */
    @Test
    void createAttemptAllowedForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/createExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Без ролей нельзя начать решать. */
    @Test
    void createAttemptForbiddenForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.USER_WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/createExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Чужой exerciseId со своим courseId отсекается проверкой связи упражнения с курсом. */
    @Test
    void createAttemptRejectedForExerciseOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/createExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.OTHER_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isConflict());
    }

    /** Отладочная попытка требует EDIT_EXERCISE. */
    @Test
    void createDebugAttemptForbiddenForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/createDebugExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** У ассистента EDIT_EXERCISE нет. */
    @Test
    void createDebugAttemptForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/createDebugExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Владелец читает свою попытку. */
    @Test
    void getAttemptAllowedForItsOwner() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExerciseAttempt")
                .param("attemptId", String.valueOf(attempt.getId())));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Преподаватель читает чужие попытки своего курса по EDIT_EXERCISE. */
    @Test
    void getAttemptAllowedForCourseTeacher() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExerciseAttempt")
                .param("attemptId", String.valueOf(attempt.getId())));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** У ассистента EDIT_EXERCISE нет, чужие попытки закрыты. */
    @Test
    void getAttemptForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExerciseAttempt")
                .param("attemptId", String.valueOf(attempt.getId())));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Студент не видит попыток других студентов. */
    @Test
    void getAttemptForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExerciseAttempt")
                .param("attemptId", String.valueOf(attempt.getId())));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Попытка вне курса проверяется в GLOBAL-области. */
    @Test
    void getGlobalPoolAttemptAllowedForItsOwner() throws Exception {
        // Arrange.
        var attempt = createGlobalPoolAttempt();
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExerciseAttempt")
                .param("attemptId", String.valueOf(attempt.getId())));

        // Assert.
        result.andExpect(status().isOk());
    }
}
